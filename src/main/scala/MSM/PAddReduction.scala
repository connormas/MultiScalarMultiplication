package MSM

import chisel3._
import chisel3.util.log2Ceil
import chisel3.util.RegEnable

class PAddReduction(numPoints: Int, pw: Int, a: Int, p: Int) extends Module {
  val io = IO(new Bundle {
    val load = Input(Bool())
    val xs = Input(Vec(numPoints, SInt(pw.W)))
    val ys = Input(Vec(numPoints, SInt(pw.W)))
    val outx = Output(SInt(pw.W))
    val outy = Output(SInt(pw.W))
    val valid = Output(Bool())
  })

  val count = RegInit(1.U(log2Ceil(numPoints).W))
  val xreg = RegInit(io.xs(0.U))
  val yreg = RegInit(io.ys(0.U))
  val xregp2 = RegInit(io.xs(1.U))
  val yregp2 = RegInit(io.ys(1.U))
  val pa = Module(new PointAddition(pw))
  val validBit = RegEnable(false.B, false.B, io.load)
  validBit := validBit || io.load

  pa.io.a := a.S
  pa.io.p := p.S
  pa.io.p1x := io.xs(0.U)
  pa.io.p1y := io.ys(0.U)
  pa.io.p2x := io.xs(1.U)
  pa.io.p2y := io.ys(1.U)
  xreg := io.xs(0.U)
  xreg := io.xs(0.U)
  yregp2 := io.ys(1.U)
  yregp2 := io.ys(1.U)
  pa.io.load := RegNext(io.load) || RegNext(pa.io.valid)
  //pa.io.load := io.load

  val encounteredInf = pa.io.valid && pa.io.outx === 0.S && pa.io.outy === 0.S


  // update when necessary
  when(io.load && count === 1.U) {
    pa.io.p1x := io.xs(0.U)
    pa.io.p1y := io.ys(0.U)
    pa.io.p2x := io.xs(1.U)
    pa.io.p2y := io.ys(1.U)
    xreg := io.xs(0.U)
    yreg := io.ys(0.U)
  } .elsewhen (encounteredInf && count < numPoints.U) {
    printf("made it here\n")
    xreg := 0.S
    yreg := 0.S
    pa.io.p1x := xreg
    pa.io.p1y := yreg
    pa.io.p2x := io.xs(count)
    pa.io.p2y := io.ys(count)
    count := count
  } .elsewhen (count < numPoints.U) { // MultsComplete, time to sum up
    //xreg := Mux(count === 1.U, io.xs(0.U), )
    //yreg := Mux(count === 1.U, io.ys(0.U))
    pa.io.p1x := xreg
    pa.io.p1y := yreg
    pa.io.p2x := io.xs(count)
    pa.io.p2y := io.ys(count)
    when (pa.io.valid) {// && !RegNext(RegNext(encounteredInf))) {
      xreg := pa.io.outx
      yreg := pa.io.outy
      pa.io.p2x := io.xs(count + 1.U)
      pa.io.p2y := io.ys(count + 1.U)
      count := count + 1.U
    }
  }

  // assign outputs (nothing meaningful right now)
  io.outx := pa.io.outx
  io.outy := pa.io.outy
  io.valid := (count === numPoints.U - 1.U) && pa.io.valid
  when (io.valid) {
    count := 1.U
  }

  // debugging
  //printf(p"padd reduction -> load=${io.load} count=${count}, x,y=(${xreg},${yreg}), p1(${pa.io.p1x},${pa.io.p1y}), p2(${pa.io.p2x}${pa.io.p2y}) pa.valid=${pa.io.valid} paout(${pa.io.outx},${pa.io.outy})\n")
  printf(p"padd reduction -> count=${count}, load=${io.load} pa.load=${pa.io.load}, x,y=(${xreg},${yreg}), p1(${pa.io.p1x},${pa.io.p1y}), p2(${pa.io.p2x}${pa.io.p2y}) pa.valid=${pa.io.valid} paout(${pa.io.outx},${pa.io.outy})\n")

}
