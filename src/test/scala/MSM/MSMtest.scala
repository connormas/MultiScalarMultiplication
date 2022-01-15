package MSM

import PippengerModel.{EllipticCurve, Point}
import chisel3._
import chisel3.tester._
import chisel3.tester.RawTester.test
import org.scalatest.FreeSpec
import chisel3.experimental.BundleLiterals._


class MSMtest extends FreeSpec with ChiselScalatestTester {


  "PointAddition Tests - manual tests" in {
    test (new PointAddition(32)) { dut =>
      dut.io.a.poke(0.S)
      dut.io.p.poke(17.S)
      dut.io.p1x.poke(15.S)
      dut.io.p1y.poke(13.S)
      dut.io.p2x.poke(5.S)
      dut.io.p2y.poke(8.S)
      dut.io.load.poke(true.B)
      dut.clock.step(1)
      dut.io.load.poke(false.B)
      dut.clock.step(1)
      while ((dut.io.valid.peek().litValue() == 0)) dut.clock.step(1)
      dut.clock.step(5)
      dut.io.outx.expect(10.S)
      dut.io.outy.expect(15.S)

      dut.io.a.poke(0.S)
      dut.io.p.poke(17.S)
      dut.io.p1x.poke(3.S)
      dut.io.p1y.poke(0.S)
      dut.io.p2x.poke(6.S)
      dut.io.p2y.poke(11.S)
      dut.io.load.poke(true.B)
      dut.clock.step(1)
      dut.io.load.poke(false.B)
      dut.clock.step(1)
      while ((dut.io.valid.peek().litValue() == 0)) dut.clock.step(1)
      dut.clock.step(5)
      dut.io.outx.expect(12.S)
      dut.io.outy.expect(1.S)
    }
  }

  "PointAddition Tests - more extensive, utilize functional model" in {
    test (new PointAddition(32)) { dut =>
      val p1707 = new EllipticCurve(0, 7, 17)
      val g = new Point(15, 13, p1707) // generator point
      for (i <- 2 until 17) {
        val t = g * i
        val r = g + t
        println(s"${i}. now testing..")
        g.print()
        t.print()
        r.print()
        println()
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