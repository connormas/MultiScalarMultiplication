package MSM

import chisel3._
import chisel3.util.RegEnable
import chisel3.util.log2Ceil
import chisel3.util._

/**
 * Top Level Multi-Scalar Multiplication Module
 * This module requests input from memory and uses that data for its msm computation.
 * It instantiates Point Mult modules and a Point Addition Reduction modules to sum
 * up the results from all the multiplications.
 *
 * @param requestsize: number of scalars/points to request at a time
 * @param numPMmodules: number of PMult modules to request at a time
 * @param a: coefficient from definition of elliptic curve
 * @param p: prime modulus from definition of elliptic curve
 *  */

object TopLevelMSM {
  val idle :: multiplying :: reduction :: Nil = Enum(3)
}

class TopLevelMSM(pw: Int, sw: Int, a: Int, p: Int,
                  requestsize: Int, numPMmodules: Int) extends Module {
  val io = IO(new Bundle {
    val complete = Input(Bool())
    val load = Input(Bool())
    val pointsx = Input(Vec(requestsize, SInt(pw.W)))
    val pointsy = Input(Vec(requestsize, SInt(pw.W)))
    val scalars = Input(Vec(requestsize, SInt(sw.W)))
    val valid = Output(Bool())
    val outx = Output(SInt(pw.W))
    val outy = Output(SInt(pw.W))
  })

  // import states
  import TopLevelMSM._
  val state = RegInit(idle)


  // make Seq of Regs, holding Points and Scalars
  val xregseq = io.pointsx map { x => RegEnable(x, 0.S, io.load) }
  val yregseq = io.pointsy map { y => RegEnable(y, 0.S, io.load) }
  val sregseq = io.scalars map { s => RegEnable(s, 0.S, io.load) }
  val completebits = Reg(Vec(requestsize, Bool()))
  when(io.load) {
    for (i <- 0 until requestsize) {
      completebits(i) := false.B
    }
  }


  // Seq of PointMult Modules
  // connect all point mult modules
  // set completebit and capture output when a mult is complete
  val PointMults = Seq.fill(requestsize)(Module(new PMNaive(pw, sw)))
  PointMults.zipWithIndex foreach { case (pm, i) =>
    pm.io.a := a.S
    pm.io.p := p.S
    pm.io.px := xregseq(i)
    pm.io.py := yregseq(i)
    pm.io.s := sregseq(i)
    pm.io.load := RegNext(RegNext(io.load))
    when ((pm.io.valid)  && !completebits(i)) {
      completebits(i) := true.B
      xregseq(i) := pm.io.outx // capture outputs
      yregseq(i) := pm.io.outy
    }
  }


  // Point Addition Reduction Module and check completebits array
  val par = Module(new PAddReduction(numPMmodules, pw, a, p))
  PointMults.zipWithIndex foreach { case (pm, i) =>
    par.io.xs(i) := xregseq(i)
    par.io.ys(i) := yregseq(i)
  }
  val readyforreduction = completebits reduce {_ && _}
  when (RegNext(readyforreduction)) {
    for (i <- 0 until requestsize) {
      completebits(i) := false.B
    }
  }
  par.io.load := RegNext(!readyforreduction && RegNext(readyforreduction))

  // val outputregs
  val outregx = RegInit(0.S(pw.W))
  val outregy = RegInit(0.S(pw.W))
  io.outx := outregx
  io.outy := outregy
  io.valid := false.B

  // main switch statement
  switch (state) {
    is (idle) {
      io.valid := false.B
      when (io.load) {
        state := multiplying
      }
    }
    is (multiplying) {
      when (readyforreduction) {
        state := reduction
      }
    }
    is (reduction) {
      outregx := par.io.outx
      outregy := par.io.outy
      when (RegNext(par.io.valid)) {
        state := idle
        io.valid := true.B
      }
    }
  }

  // debugging
  /*when (RegNext(RegNext(io.load))) {
    printf("IO.LOAD: ")
    xregseq zip yregseq zip sregseq foreach { case((x,y), s) => printf(p"(${x},${y})${s} ")}
    printf("\n")
  }*/

  /*when (readyforreduction) {
    printf("READYFORREDUCTION: ")
    xregseq zip yregseq foreach { case(x,y) => printf(p"(${x},${y}) ")}
    printf("\n")
  }*/

  //printf(p"${completebits} load=${io.load} valid${io.valid} paddreduc.load(${par.io.load}) result(${par.io.outx},${par.io.outy})\n")
}