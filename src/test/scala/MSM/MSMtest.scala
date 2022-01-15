package MSM

import PippengerModel.{EllipticCurve, Point}
import chisel3._
import chisel3.tester._
import chisel3.tester.RawTester.test
import org.scalatest.FreeSpec
import chisel3.experimental.BundleLiterals._


class testfunctions {
  def PointAdditionTest(dut: PointAddition, a: Int, p: Int,
                        p1x: Int, p1y: Int, p2x: Int, p2y: Int,
                        rx: Int, ry: Int): Unit = {
    dut.io.a.poke(a.S)
    dut.io.p.poke(p.S)
    dut.io.p1x.poke(p1x.S)
    dut.io.p1y.poke(p1y.S)
    dut.io.p2x.poke(p2x.S)
    dut.io.p2y.poke(p2y.S)
    dut.io.load.poke(true.B)
    dut.clock.step(1)
    dut.io.load.poke(false.B)
    dut.clock.step(1)
    while ((dut.io.valid.peek().litValue() == 0)) dut.clock.step(1)
    dut.io.outx.expect(rx.S)
    dut.io.outy.expect(ry.S)
  }
}


class MSMtest extends FreeSpec with ChiselScalatestTester {

  val t = new testfunctions()
  "PointAddition Tests - manual tests" in {
    test (new PointAddition(32)) { dut =>
      t.PointAdditionTest(dut,  0, 17, 15, 13, 15, 13,  2, 10) // point double
      t.PointAdditionTest(dut,  0, 17,  0,  0,  6, 11,  6, 11) // point at inf
      t.PointAdditionTest(dut,  0, 17,  3,  0,  6, 11, 12,  1)
    }
  }

  "PointAddition Tests - more extensive, utilize functional model" in {
    test (new PointAddition(32)) { dut =>
      val p1707 = new EllipticCurve(0, 7, 17)
      val g = new Point(15, 13, p1707) // generator point
      for (i <- 1 until 17) {
        val t = g * i
        val r = g + t
        //println(s"${i}. now testing..")
        //g.print()
        //t.print()
        //r.print()
        //println()
        dut.io.a.poke(g.curve.a.S)
        dut.io.p.poke(g.curve.p.S)
        dut.io.p1x.poke(g.x.S)
        dut.io.p1y.poke(g.y.S)
        dut.io.p2x.poke(t.x.S)
        dut.io.p2y.poke(t.y.S)
        dut.io.load.poke(true.B)
        dut.clock.step(1)
        dut.io.load.poke(false.B)
        dut.clock.step(1)
        while ((dut.io.valid.peek().litValue() == 0)) dut.clock.step(1)
        dut.io.outx.expect(r.x.S)
        dut.io.outy.expect(r.y.S)
      }
    }
  }



  /*"ModularInverse should find the mod inverse" in {
    test(new ModularInverse(32)) { dut =>
      dut.io.a.poke(3.S)
      dut.io.p.poke(7.S)
      dut.io.load.poke(true.B)
      dut.clock.step(1)
      dut.io.load.poke(false.B)
      dut.clock.step(1)
      while ((dut.io.valid.peek().litValue() == 0)) dut.clock.step(1)
      dut.io.out.expect(5.S)

      dut.io.a.poke(4.S)
      dut.io.p.poke(17.S)
      dut.io.load.poke(true.B)
      dut.clock.step(1)
      dut.io.load.poke(false.B)
      dut.clock.step(1)
      while ((dut.io.valid.peek().litValue() == 0)) dut.clock.step(1)
      dut.io.out.expect(13.S)

      dut.io.a.poke(2.S)   // case where there is no inverse, returns 0
      dut.io.p.poke(6.S)
      dut.io.load.poke(true.B)
      dut.clock.step(1)
      dut.io.load.poke(false.B)
      dut.clock.step(1)
      while ((dut.io.valid.peek().litValue() == 0)) dut.clock.step(1)
      dut.io.out.expect(0.S)

      dut.io.a.poke(-10.S) // case where a is negative
      dut.io.p.poke(17.S)
      dut.io.load.poke(true.B)
      dut.clock.step(1)
      dut.io.load.poke(false.B)
      dut.clock.step(1)
      while ((dut.io.valid.peek().litValue() == 0)) dut.clock.step(1)
      dut.io.out.expect(5.S)
    }
  }*/
}