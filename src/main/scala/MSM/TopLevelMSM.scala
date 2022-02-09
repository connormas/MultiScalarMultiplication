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

  // Seq of RegEnables, holding Points and Scalars (might need Vecs)
  val xregseq: Seq[Data] = (0 until requestsize) map (i => RegEnable(io.pointsx(i), 0.S, io.load))
  //val pointsxvec = VecInit(xregseq)
  val yregseq: Seq[Data] = (0 until requestsize) map (i => RegEnable(io.pointsy(i), 0.S, io.load))
  //val pointsyvec = VecInit(yregseq)
  val sregseq: Seq[Data] = (0 until requestsize) map (i => RegEnable(io.scalars(i), 0.S, io.load))
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
    pm.io.load := RegNext(io.load)
  }

  // Point Addition Reduction Module
  val par = Module(new PAddReduction(numPMmodules, pw, a, p))
  PointMults.zipWithIndex foreach { case (pm, i) =>
    par.io.xs(i) := pm.io.outx
    par.io.ys(i) := pm.io.outy
  }
  //val MultsComplete = PointMults.fold(true.B)( (sig, pm) => sig && pm.io.valid)
  var MultsComplete = true.B
  PointMults foreach { pm =>
    MultsComplete = MultsComplete && pm.io.valid
  }

  par.io.load := RegNext(MultsComplete) // load when all PMults are complete

  io.outx := par.io.outx
  io.outy := par.io.outy
  io.valid := RegNext(io.load)

  // debugging
  xregseq foreach (x => printf(p"${x}"))
  printf("\n")
  //printf(p"TLMSM -> load=${io.load} multscomplet=${MultsComplete}\n")
}
