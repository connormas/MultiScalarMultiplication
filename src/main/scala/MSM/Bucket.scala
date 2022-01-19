package MSM

import chisel3._

/** TODO
 * - should this take as input one point at a time? a few points at a time?
 * */

class Bucket(pw: Int, sw: Int) extends Module {
  val io = IO(new Bundle {
    val px =   Input(UInt(pw.W))
    val py =   Input(UInt(pw.W))
    val s =    Input(UInt(sw.W))
    val load = Input(Bool())
    val mult = Input(Bool())
    val outx = Output(UInt(pw.W))
    val outy = Output(UInt(pw.W))
  })

  val x = RegInit(UInt(pw.W))
  val y = RegInit(UInt(pw.W))
  val s = RegInit(UInt(pw.W))
  val numbuckets = math.pow(2, sw)
  val B = Reg(Vec(numbuckets.toInt, UInt(sw.W))) // buckets, one for each possible s
  val pa = Module(new PointAddition(pw))

  // latch point into regs
  when (io.load) {
    x := io.px
    y := io.py
    s := io.s
  }

}
