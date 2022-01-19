package MSM

import chisel3._

class PointMult(pw: Int, sw: Int) extends Module {
  val io = IO(new Bundle {
    val a =  Input(SInt(pw.W))
    val p =  Input(SInt(pw.W))
    val px = Input(SInt(pw.W))
    val py = Input(SInt(pw.W))
    val s =  Input(SInt(sw.W))
    val outx = Output(SInt(pw.W))
    val outy = Output(SInt(pw.W))
    val valid = Output(Bool())
  })

  val padd = Module(new PointAddition(pw))
  padd.io.p1x := io.px
  padd.io.p1y := io.py
  padd.io.p2x := io.px
  padd.io.p2y := io.py
  padd.io.load := true.B
  io.outx := padd.io.outx
  io.outy := padd.io.outy

  when (io.s > 0.S && padd.io.valid) {
    io.s := io.s - 1.S
    padd.io.p2x := padd.io.outx
    padd.io.p2y := padd.io.outy
  }

  // assert valid signal
  io.valid := io.s === 0.S
}
