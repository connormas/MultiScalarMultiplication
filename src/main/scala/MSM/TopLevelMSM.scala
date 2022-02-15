package MSM

import chisel3._
import chisel3.util.RegEnable
import chisel3.util.log2Ceil

/** top level msm module. This module requests input from memory and uses that
 *  data fro its msm computation. It instantiates Point Mult modules and
 *
 *  @param requestsize: number of scalars/points to request at a time
 *  @param numPMmodules: number of PMult modules to request at a time
 *  @param a: coefficient from definition of elliptic curve
 *  @param p: prime modulus from definition of elliptic curve
 *  */
class TopLevelMSM(pw: Int, sw: Int, a: Int, p: Int,
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
  val par = Module(new PAddReduction(numPMmodules, pw, a, p))
  PointMults.zipWithIndex foreach { case (pm, i) =>
    par.io.xs(i) := xregseq(i)
    par.io.ys(i) := yregseq(i)
  }

  // count up number of complete mults
  val MultsComplete = RegInit(0.U(log2Ceil(numPMmodules).W))
  PointMults.zipWithIndex foreach { case (pm, i) =>
    when (pm.io.valid) {
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
  par.io.load := RegNext(curr === 0.U && prev === numPMmodules.U - 1.U)

  // latch reduction results
  val xoutput = RegEnable(par.io.outx, 0.S, par.io.valid)
  val youtput = RegEnable(par.io.outy, 0.S, par.io.valid)

  // reset sum logic
  val resetsum = Reg(Bool())
  resetsum := io.complete && io.load  && !RegNext(io.complete) && !RegNext(io.load) // edge detector
  val resetonthiscycle = RegInit(false.B)
  resetonthiscycle := resetonthiscycle || resetsum

  /*val pa = Module(new PointAddition(pw))
  pa.io.a := a.S
  pa.io.p := p.S
  pa.io.p1x := xoutput
  pa.io.p1x := youtput
  pa.io.p2x := par.io.outx
  pa.io.p2y := par.io.outy
  pa.io.load := RegNext(par.io.valid)*/


  io.outx := xoutput
  io.outy := youtput

  io.valid := RegNext(par.io.valid)

  // debugging
  /*printf(p"MultsComplete=${MultsComplete}  ")
  PointMults foreach (pm => printf(p"| ${pm.io.valid} (${pm.io.outx}${pm.io.outy}) |"))
  printf("\n")
  xregseq zip yregseq foreach { case (x, y) => printf(p"(${x},${y}) ")}
  printf("\n")
  printf(p"par.load=${par.io.load} -> ")
  par.io.xs.zip(par.io.ys) foreach { case (x, y) => printf(p"(${x},${y}) ")}
  printf(p" -> (${par.io.outx},${par.io.outy})\n")*/
  //printf(p"TLMSM -> load=${io.load} multscomplet=${MultsComplete}\n")
}
