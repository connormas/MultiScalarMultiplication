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
  val delayedLoad = RegNext(io.load)

  // instantiate PointAddition module and make connections
  val padd = Module(new PointAddition(pw))
  padd.io.a := io.a
  padd.io.p := io.p
  padd.io.p1x := x
  padd.io.p1y := y
  padd.io.load := delayedLoad || (RegNext(padd.io.valid) && !RegNext(delayedLoad)) // takes one cycle to latch (x,y) or intermed results

  padd.io.p2x := Mux(delayedLoad, x, padd.io.outx) // initial values? x else intermed results
  padd.io.p2y := Mux(delayedLoad, y, padd.io.outy)

  // update inputs into padd module
  when (s > 0.S && padd.io.valid && !io.load && !delayedLoad) {
    s := s - 1.S
    padd.io.p2x := padd.io.outx
    padd.io.p2y := padd.io.outy
  }

  // regs to hold result, assign outputs, assert valid
  val xres = RegEnable(padd.io.outx, 0.S, padd.io.valid)
  val yres = RegEnable(padd.io.outy, 0.S, padd.io.valid)
  io.outx := xres
  io.outy := yres
  io.valid := s === 1.S && padd.io.valid

  // update padd inputs, decrement s
  when (s > 1.S && padd.io.valid && !io.load && !delayedLoad) {
    s := s - 1.S
    padd.io.p2x := padd.io.outx
    padd.io.p2y := padd.io.outy
  }


  // debugging
  //printf(p"--- inside pmult x=${x} y=${y}, xres=${xres}, yres=${yres}, s=${s}, io.load=${io.load} and dl=${delayedLoad} and padd.valid=${padd.io.valid} and dv=${RegNext(padd.io.valid)}, valid=${io.valid}\n")
}

