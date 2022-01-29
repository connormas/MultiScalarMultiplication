package MSM

import chisel3._
import chisel3.util.RegEnable

/** Point Multiplication Module (Bit Serial)
 * This module performs scalar point multiplication but utilized the
 * double and add method. The method is as follows:
 * - observe scalar in binary form.
 * - for each bit, starting from lsb -> msb
 *  - perform a point double on P and add it to running total if bit == 1
 * */
class PMBitSerial(pw: Int, sw: Int) extends Module {
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
  val count = Reg(UInt(sw.W))
  count := Mux(io.load, 0.U, count)
  val pdblx = RegEnable(io.px, 0.S, io.load)
  val pdbly = RegEnable(io.py, 0.S, io.load)

  //val (count, wrap) = Counter(true.B, sw)

  // instantiate PointAddition module and make connections
  val pdbl = Module(new PointAddition(pw))
  pdbl.io.a := io.a
  pdbl.io.p := io.p
  pdbl.io.p1x := pdblx
  pdbl.io.p1y := pdbly
  pdbl.io.p2x := pdblx
  pdbl.io.p2y := pdbly
  pdbl.io.load := delayedLoad || (RegNext(pdbl.io.valid) && !RegNext(delayedLoad))

  // control signals
  val update = pdbl.io.valid && s(count) // update when pdbl complete

  // instatiations
  val xres = RegEnable(pdblx, 0.S, update)
  val yres = RegEnable(pdbly, 0.S, update)


  io.outx := xres
  io.outy := yres
  // next pdbl
  val stillcounting = RegNext(count < (sw-1).U) && !io.load
  when (stillcounting) {
    count := count +& 1.U
    pdbl.io.p2x := pdbl.io.outx
    pdbl.io.p2y := pdbl.io.outy
  }

  // assert valid signal
  io.valid := !stillcounting

  printf(p"PMBS -- sc=${stillcounting}load=${io.load} pdbl=(${pdbl.io.outx},${pdbl.io.outy}), pdbl=(${pdblx},${pdbly}),count=${count}, s(count)=${s(count)}\n")
}
