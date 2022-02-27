package MSM

import chisel3._
import chisel3.util.RegEnable
import chisel3.util.log2Ceil
import chisel3.util._

/** top level msm module. This module requests input from memory and uses that
 *  data fro its msm computation. It instantiates Point Mult modules and
 *
 *  @param requestsize: number of scalars/points to request at a time
 *  @param numPMmodules: number of PMult modules to request at a time
 *  @param a: coefficient from definition of elliptic curve
 *  @param p: prime modulus from definition of elliptic curve
 *  */
class TopLevelMSM2(pw: Int, sw: Int, a: Int, p: Int,
                  requestsize: Int, numPMmodules: Int) extends Module {
  val io = IO(new Bundle {
    val complete = Input(Bool())
    val load =    Input(Bool())
    val pointsx = Input(Vec(requestsize, SInt(pw.W)))
    val pointsy = Input(Vec(requestsize, SInt(pw.W)))
    val scalars = Input(Vec(requestsize, SInt(sw.W)))
    val valid =   Output(Bool())
    val outx =    Output(SInt(pw.W))
    val outy =    Output(SInt(pw.W))
  })

  // make Seq of Regs, holding Points and Scalars (will need Vecs when decoupling)
  val xregseq = io.pointsx map {x => RegEnable(x, 0.S, io.load)}
  //val pointsxvec = VecInit(xregseq)
  val yregseq = io.pointsy map {y => RegEnable(y, 0.S, io.load)}
  //val pointsyvec = VecInit(yregseq)
  val sregseq = io.scalars map {s => RegEnable(s, 0.S, io.load)}
  //val scalarsvec = VecInit(sregseq)
  val completebits = Reg(Vec(requestsize, Bool()))
  when (io.load) {
    for (i <- 0 until requestsize) {
      completebits(i) := false.B
    }
  }


  // Seq of PointMult Modules, may need to become a Vec later
  // make case class or companion object to inline PM module instantiation
  val PointMults = Seq.fill(requestsize)(Module(new PMNaive(pw, sw)))
  PointMults.zipWithIndex foreach { case (pm, i) =>
    pm.io.a := a.S
    pm.io.p := p.S
    pm.io.px := xregseq(i)
    pm.io.py := yregseq(i)
    pm.io.s := sregseq(i)
    pm.io.load := RegNext(RegNext(io.load))
  }


  // Point Addition Reduction Module
  val par = Module(new PAddReduction2(numPMmodules, pw, a, p))
  PointMults.zipWithIndex foreach { case (pm, i) =>
    par.io.xs(i) := xregseq(i)
    par.io.ys(i) := yregseq(i)
  }

  // count up number of complete mults
  val MultsComplete = RegInit(0.U(log2Ceil(numPMmodules).W))
  PointMults.zipWithIndex foreach { case (pm, i) =>
    when ((pm.io.valid)  && !completebits(i)) {
      completebits(i) := true.B
      MultsComplete := MultsComplete + 1.U
      xregseq(i) := pm.io.outx // capture outputs
      yregseq(i) := pm.io.outy
    }
  }

  // start reduction when all mults are complete
  // use edge detector (more of a 'wrap detector')
  val curr = Reg(UInt(log2Ceil(numPMmodules).W))
  val prev = Reg(UInt(log2Ceil(numPMmodules).W))
  curr := MultsComplete
  prev := curr
  //par.io.load := RegNext(curr === 0.U && prev === numPMmodules.U - 1.U)
  val readyforreduction = completebits reduce {_ && _}
  when (RegNext(readyforreduction)) {
    for (i <- 0 until requestsize) {
      completebits(i) := false.B
    }
  }
  par.io.load := RegNext(readyforreduction)


  // latch reduction results
  val xreduc = RegEnable(par.io.outx, 0.S, par.io.valid)
  val yreduc = RegEnable(par.io.outy, 0.S, par.io.valid)

  // reset sum logic
  val resetsum = Reg(Bool())
  resetsum := io.complete && io.load  && !RegNext(io.complete) && !RegNext(io.load) // edge detector
  val resetonthiscycle = RegInit(false.B)
  resetonthiscycle := resetonthiscycle || resetsum

  // latch final sum results
  val xoutput = Reg(SInt(pw.W))
  val youtput = Reg(SInt(pw.W))
  xoutput := 0.S
  youtput := 0.S

  val pa = Module(new PointAddition(pw))
  pa.io.a := a.S
  pa.io.p := p.S
  pa.io.p1x := xoutput
  pa.io.p1y := youtput
  pa.io.p2x := xreduc
  pa.io.p2y := yreduc
  pa.io.load := RegNext(RegNext(par.io.valid))

  when (pa.io.valid) {
    xoutput := pa.io.outx
    youtput := pa.io.outy
  }

  // reset when next msm computation begins
  when (resetonthiscycle && io.load && !io.complete) {
    resetonthiscycle := false.B // issue when next one is single load
    resetsum := false.B
    xoutput := 0.S
    youtput := 0.S
  }

  io.outx := xoutput
  io.outy := youtput

  //io.valid := RegNext(par.io.valid)
  io.valid := RegNext(pa.io.valid) //&& resetonthiscycle

  /*when (readyforreduction) {
    printf("\nmultscomplete: ")
    xregseq zip yregseq foreach { case(x, y) => printf(p"(${x}${y}) ")}
    printf(p"\n")
  }*/

  printf(p"${completebits} load=${io.load} valid${io.valid}\n")

}



/** second attempt this time with a state machine
 * */

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


  // make Seq of Regs, holding Points and Scalars (will need Vecs when decoupling)
  val xregseq = io.pointsx map { x => RegEnable(x, 0.S, io.load) }
  val yregseq = io.pointsy map { y => RegEnable(y, 0.S, io.load) }
  val sregseq = io.scalars map { s => RegEnable(s, 0.S, io.load) }
  val completebits = Reg(Vec(requestsize, Bool()))
  when(io.load) {
    for (i <- 0 until requestsize) {
      completebits(i) := false.B
    }
  }


  // Seq of PointMult Modules, may need to become a Vec later
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
      //printf(p"MULTIPLIER ${i} finished, (${pm.io.outx},${pm.io.outy})\n")
    }
  }


  // Point Addition Reduction Module and check completebits
  val par = Module(new PAddReduction2(numPMmodules, pw, a, p))
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
        //printf("moving to working\n")
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