package MSM

import chisel3._
import chisel3.util.RegEnable

class PointMult(pw: Int, sw: Int) extends Module {
  val io = IO(new Bundle {
    val a =  Input(SInt(pw.W))
    val p =  Input(SInt(pw.W))
    val px = Input(SInt(pw.W))
    val py = Input(SInt(pw.W))
    val s =  Input(SInt(sw.W))
    val load = Input(Bool())
    val valid = Output(Bool())
    val outx = Output(SInt(pw.W))
    val outy = Output(SInt(pw.W))
  })


  // regs to latch x, y, and s values, delay load by 1 cycle
  val x = RegEnable(io.px, 0.S, io.load)
  val y = RegEnable(io.py, 0.S, io.load)
  val s = RegEnable(io.s, 0.S, io.load)
  val l = RegInit(io.load)

  // instantiate PointAddition module and make connections
  val padd = Module(new PointAddition(pw))
  padd.io.p1x := x
  padd.io.p1y := y
  padd.io.p2x := x
  padd.io.p2y := y
  padd.io.load := l
  padd.io.a := io.a
  padd.io.p := io.p


  // regs to hold result, assign outputs
  val xres = Reg(SInt(pw.W))
  val yres = Reg(SInt(pw.W))
  io.outx := xres
  io.outy := yres
  padd.io.p2x := xres
  padd.io.p2y := yres


  when (s > 0.S && padd.io.valid) {
    s := s - 1.S
    xres := padd.io.outx
    yres := padd.io.outy
  }

  // assert valid signal
  when (s === 1.S) {
    io.outx := x
    io.outy := y
    io.valid := true.B
  } .otherwise {
    io.valid := s === 0.S
  }
}

