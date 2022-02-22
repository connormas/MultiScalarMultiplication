package MSM

import chisel3._
import chisel3.util.log2Ceil
import chisel3.util.RegEnable
import chisel3.util._

/**
 * Point Addition Reduction Module
 * This module takes in a number of points to sum up at a certain time and
 * performs an addition reduction. This is done by instantiating a Point Addition
 * module and using it repeatedly to end up with a final sum. This RTL could
 * certaily get clened up more but due to current (2-9-22) time constraints,
 * this will have to do for now.
 * */
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
  val pa = Module(new PointAddition(pw))
  val validBit = RegEnable(false.B, false.B, io.load)
  validBit := validBit || io.load

  // latch points into regs
  val xvec = Reg(Vec(numPoints, SInt(pw.W)))
  val yvec = Reg(Vec(numPoints, SInt(pw.W)))
  when (io.load) {
    for (i <- 0 until numPoints) {
      xvec(i) := io.xs(i)
      yvec(i) := io.ys(i)
    }
  }


  pa.io.a := a.S
  pa.io.p := p.S
  pa.io.p1x := io.xs(0.U)        // making changes here
  pa.io.p1y := io.ys(0.U)
  pa.io.p2x := io.xs(1.U)
  pa.io.p2y := io.ys(1.U)
  //pa.io.p1x := xvec(0)
  //pa.io.p1y := yvec(0)
  //pa.io.p2x := xvec(1)
  //pa.io.p2y := yvec(1)

  //xreg := io.xs(0.U)     // making changes here
  //yreg := io.ys(0.U)
  xreg := xvec(0)
  yreg := yvec(0)

  pa.io.load := RegNext(io.load) || RegNext(pa.io.valid)

  // debugging
  when ((io.load) || RegNext(io.load)) {
    io.xs zip io.ys foreach { case (x, y) => printf(p"(${x},${y}) ") }
    printf(p" PADDREDUCTION count=${count}\n")
    xvec zip yvec foreach { case (x, y) => printf(p"(${x},${y}) ") }
    printf(p" PADDREDUCTION count=${count}\n")
  }


  // did we end up with the Point at Infinity?
  val encounteredInf = pa.io.valid && pa.io.outx === 0.S && pa.io.outy === 0.S
  val infinput = io.xs(count) === 0.S && io.ys(count) === 0.S

  // update when necessary
  when(io.load && count === 1.U) {
    pa.io.p1x := io.xs(0.U)         // making changes here
    pa.io.p1y := io.ys(0.U)
    pa.io.p2x := io.xs(1.U)
    pa.io.p2y := io.ys(1.U)
    /*pa.io.p1x := xvec(0)
    pa.io.p1y := yvec(0)
    pa.io.p2x := xvec(1)
    pa.io.p2y := yvec(1)*/
    xreg := io.xs(0.U)
    yreg := io.ys(0.U)
  } .elsewhen (encounteredInf && count < numPoints.U) {
    //printf("MADE IT HERE----------------------------------------------\n")
    xreg := 0.S
    yreg := 0.S
    pa.io.p1x := xreg
    pa.io.p1y := yreg
    //pa.io.p2x := io.xs(count)       // making changes here
    //pa.io.p2y := io.ys(count)
    pa.io.p2x := xvec(count)
    pa.io.p2y := yvec(count)
    count := count
  } .elsewhen (infinput && count === numPoints.U - 2.U) {
    //printf("\nTHIS CASE ----------------------------------------------\n\n")
    pa.io.p1x := xreg
    pa.io.p1y := yreg
    //pa.io.p2x := io.xs(count + 1.U) // making changes here
    //pa.io.p2y := io.ys(count + 1.U)
    pa.io.p2x := xvec(count + 1.U)
    pa.io.p2y := yvec(count + 1.U)
    count := count + 1.U
  } .elsewhen (count < numPoints.U) {
    pa.io.p1x := xreg
    pa.io.p1y := yreg
    //pa.io.p2x := io.xs(count)   // making changes here
    //pa.io.p2y := io.ys(count)
    pa.io.p2x := xvec(count)
    pa.io.p2y := yvec(count)
    when (pa.io.valid) {
      xreg := pa.io.outx
      yreg := pa.io.outy
      //pa.io.p2x := io.xs(count + 1.U) // making changes here
      //pa.io.p2y := io.ys(count + 1.U)
      pa.io.p2x := xvec(count + 1.U)
      pa.io.p2y := yvec(count + 1.U)
      count := count + 1.U
    }
  }

  // latch values into regs, assign outputs
  val xoutreg = RegEnable(pa.io.outx, 0.S, pa.io.valid)
  val youtreg = RegEnable(pa.io.outy, 0.S, pa.io.valid)
  io.outx := xoutreg
  io.outy := youtreg

  //io.outx := pa.io.outx
  //io.outy := pa.io.outy
  io.valid := RegNext((count === numPoints.U - 1.U) && pa.io.valid)
  when (count === numPoints.U - 1.U && pa.io.valid || io.load) {
    count := 1.U
  }

  // debugging
}

object PAddReduction2 {
  val idle :: working :: Nil = Enum(2)
}


class PAddReduction2(numPoints: Int, pw: Int, a: Int, p: Int) extends Module {
  val io = IO(new Bundle {
    val load = Input(Bool())
    val xs = Input(Vec(numPoints, SInt(pw.W)))
    val ys = Input(Vec(numPoints, SInt(pw.W)))
    val outx = Output(SInt(pw.W))
    val outy = Output(SInt(pw.W))
    val valid = Output(Bool())
  })

  // import states
  import PAddReduction2._


  // instantiations
  val count = RegInit(1.U(log2Ceil(numPoints).W))
  val xreg = RegInit(io.xs(0.U))
  val yreg = RegInit(io.ys(0.U))
  //val pa = Module(new PointAddition(pw))
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
      val infinput = p1inf || p2inf

      // go high with infinput, go low when valid goes back down
      val test = RegInit(false.B)
      when (infinput) {
        test := true.B
      } .elsewhen (!pa.io.valid && RegNext(pa.io.valid)) {
        test := false.B
      }

      // update inputs to PAdd Module
      when (infinput && pa.io.valid) {
        //when (p1inf && !p2inf) {
          xinter := pa.io.p2x
          yinter := pa.io.p2y
        //}
        count := count + 1.U
      } /*.elsewhen (test && !RegNext(test)) {
        count := count + 1.U
      }*/ .elsewhen (pa.io.valid && !test) {
        count := count + 1.U
        xinter := pa.io.outx
        yinter := pa.io.outy
      }

      // assert valid signal state transition
      when (pa.io.valid && count === numPoints.U - 1.U && !RegNext(infinput) && !RegNext(RegNext(infinput))) {
        state := idle
        io.valid := true.B
        io.outx := pa.io.outx
        io.outy := pa.io.outy
      }
      // debugging
      printf(p"count(${count}) inter(${xinter}${yinter}) pa.io.load=${pa.io.load}, pa.io.valid(${pa.io.valid}) inputs(${pa.io.p1x}${pa.io.p1y})(${pa.io.p2x}${pa.io.p2y}) test(${test})")
      when (infinput) {
        printf("we got an infinput")
      }
      printf("\n")
    }
  }


  //io.valid := RegNext(RegNext(RegNext(RegNext(io.load))))


  // debugging
  when ((io.load) || RegNext(io.load)) {
    io.xs zip io.ys foreach { case (x, y) => printf(p"(${x},${y}) ") }
    printf(p" PADDREDUCTION count=${count}\n")
    xvec zip yvec foreach { case (x, y) => printf(p"(${x},${y}) ") }
    printf(p" PADDREDUCTION count=${count}\n")
  }


}
