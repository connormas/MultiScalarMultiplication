package MSM

import chisel3._
import chisel3.util.RegEnable

/** top level msm module. This module requests input from memory and uses that
 *  data fro its msm computation. It instantiates Point Mult modules and
 *
 *  @param requestsize: number of scalars/points to request at a time
 *  @param numPMmodules: number of PMult modules to request at at ime
 *  @param a: coefficient from definition of elliptic curve
 *  @param p: prime modulus from definition of elliptic curve
 *  */
class TopLevelMSM(pw: Int, sw: Int, a: Int, p: Int,
                  requestsize: Int, numPMmodules: Int) extends Module {
  val io = IO(new Bundle {
    val load =    Input(Bool())
    val pointsx = Input(Vec(requestsize, SInt(pw.W)))
    val pointsy = Input(Vec(requestsize, SInt(pw.W)))
    val scalars = Input(Vec(requestsize, SInt(sw.W)))
    val valid =   Output(Bool())
    val outx =    Output(UInt(pw.W))
    val outy =    Output(UInt(pw.W))
  })

  // Vec of RegEnables, holding Points and Scalars
  val xregseq: Seq[Data] = (0 until requestsize) map (i => RegEnable(io.pointsx(i), 0.S, io.load))
  //val pointsxvec = VecInit(xregseq)
  val yregseq: Seq[Data] = (0 until requestsize) map (i => RegEnable(io.pointsy(i), 0.S, io.load))
  //val pointsyvec = VecInit(yregseq)
  val sregseq: Seq[Data] = (0 until requestsize) map (i => RegEnable(io.scalars(i), 0.S, io.load))
  //val scalarsvec = VecInit(sregseq)


  // Seq (or Vec?) of PointMult Modules
  // make case class or companion object to inline PM module instantiation
  // var PointMults: Seq[PMNaive] = List()
  val PointMults = Vec(requestsize, Module(new PMNaive(pw,sw)).io)
  for (i <- 0 until requestsize) {
    PointMults(i).a := a.S
    PointMults(i).p := p.S
    PointMults(i).px := xregseq(i)
    PointMults(i).py := yregseq(i)
    PointMults(i).s := sregseq(i)
    PointMults(i).load := RegNext(io.load)
  }

  // Point Addition module to sum up results
  var pointMultsDone = Wire(false.B)
  for (i <- 0 until requestsize) {
    pointMultsDone := pointMultsDone && PointMults(i).valid
  }

  // PointAddition Reduction
  val pa = Module(new PointAddition(pw))
  pa.io.a := a.S
  pa.io.p := p.S
  val count = RegInit(requestsize.U)
  when (pointMultsDone) {

  }
}
