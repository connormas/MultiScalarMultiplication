package MSM

import chisel3._
import chisel3.util.log2Ceil
import chisel3.util.RegEnable
import chisel3.util._

/**
 * Point Addition Reduction Module
 * This module takes in a number of points to sum up at a certain time and
 * performs an addition reduction. This is done by instantiating a Point Addition
 * module and using it repeatedly to end up with a final sum.
 *
 * @param numPoints: the number of ec points to sum up
 * @param pw: "point width" i.e. the bit width of the ec points
 * @param a: coefficient from definition of elliptic curve
 * @param p: prime modulus from definition of elliptic curve
 * */

object PAddReduction {
  val idle :: working :: Nil = Enum(2)
}

class PAddReduction(numPoints: Int, pw: Int, a: Int, p: Int) extends Module {
  val io = IO(new Bundle {
    val load = Input(Bool())
    val xs = Input(Vec(numPoints, SInt(pw.W)))
    val ys = Input(Vec(numPoints, SInt(pw.W)))
    val outx = Output(SInt(pw.W))
    val outy = Output(SInt(pw.W))
    val valid = Output(Bool())
  })

  // import states
  import PAddReduction._


  // instantiations
  val count = RegInit(1.U(log2Ceil(numPoints).W))
  val xreg = RegInit(io.xs(0.U))
  val yreg = RegInit(io.ys(0.U))
  val validBit = RegEnable(false.B, false.B, io.load)
  validBit := validBit || io.load
  val state = RegInit(idle)

  // latch points into regs
  val xvec = Reg(Vec(numPoints, SInt(pw.W)))
  val yvec = Reg(Vec(numPoints, SInt(pw.W)))
  when (io.load) {
    for (i <- 0 until numPoints) {
      xvec(i) := io.xs(i)
      yvec(i) := io.ys(i)
    }
  }

  // defaults
  io.outx := 0.S
  io.outy := 0.S
  io.valid := false.B

  // regs to hold intermediate values
  val xinter = RegEnable(xvec(0), 0.S, RegNext(io.load))
  val yinter = RegEnable(yvec(0), 0.S, RegNext(io.load))

  // main switch statement
  switch (state) {
    is (idle) {
      io.valid := false.B

      // have defaults ready
      xinter := xvec(0)
      yinter := yvec(0)
      count := 1.U

      when (io.load) {
        state := working
      }
    }
    is (working) {
      io.valid := false.B

      // instantiate the Point Addition Module
      val pa = Module(new PointAddition(pw))
      pa.io.load := RegNext(RegNext(io.load)) || RegNext(pa.io.valid)
      pa.io.a := a.S
      pa.io.p := p.S

      // assign inputs to PAdd Module
      pa.io.p1x := xinter
      pa.io.p1y := yinter
      pa.io.p2x := xvec(count)
      pa.io.p2y := yvec(count)

      // check if we have an infinite input
      val p1inf = (pa.io.p1x === 0.S && pa.io.p1y === 0.S)
      val p2inf = (pa.io.p2x === 0.S && pa.io.p2y === 0.S)
      val infinput = (p1inf || p2inf) && validBit

      // go high with infinput, go low when valid goes back down
      val test = RegInit(false.B)
      when (infinput) {
        test := true.B
      } .elsewhen (!pa.io.valid && RegNext(pa.io.valid)) {
        test := false.B
      }

      // update inputs to PAdd Module
      when (infinput && pa.io.valid) {
        when (p1inf && !p2inf) { // may need this later for other case
          xinter := pa.io.p2x
          yinter := pa.io.p2y
        } .elsewhen (!p1inf && p2inf) {
          when (count === numPoints.U - 1.U) {
            state := idle
            io.valid := true.B
            io.outx := xinter
            io.outy := yinter
          }
        }
        count := count + 1.U
      }.elsewhen (pa.io.valid && !test) {
        count := count + 1.U
        xinter := pa.io.outx
        yinter := pa.io.outy
      }

      // assert valid signal, state transition
      when (pa.io.valid && count === numPoints.U - 1.U && !test) {//&& !RegNext(infinput) && !RegNext(RegNext(infinput))) {
        state := idle
        io.valid := true.B
        io.outx := pa.io.outx
        io.outy := pa.io.outy
      }
    }
  }

  // debugging
  /*when ((io.load) || RegNext(io.load)) {
    io.xs zip io.ys foreach { case (x, y) => printf(p"(${x},${y}) ") }
    printf(p" PADDREDUCTION count=${count}\n")
    xvec zip yvec foreach { case (x, y) => printf(p"(${x},${y}) ") }
    printf(p" PADDREDUCTION count=${count}\n")
  }*/
}
