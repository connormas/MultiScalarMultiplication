package MSM

import PippengerModel.{EllipticCurve, Point}
import chisel3._
import chisel3.tester._
import chisel3.tester.RawTester.test
import org.scalatest.{FlatSpec, FreeSpec}
import chisel3.experimental.BundleLiterals._
//import chisel3.TesterDriver
//import org.scalatest._



class testfunctions {
  def PointAdditionTest(dut: PointAddition, a: Int, p: Int,
                        p1x: Int, p1y: Int, p2x: Int, p2y: Int,
                        rx: Int, ry: Int): Unit = {
    println("PA Tests starting: (" + p1x, p1y +") + (" + p2x, p2y + ") = (" + rx, ry +")")
    dut.io.a.poke(a.S)
    dut.io.p.poke(p.S)
    //val p1 = new PointBundle(32) // how to initialize values?
    //val p2 = new PointBundle(32)
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
    dut.clock.step()
    println("\n\n")
  }

  def PointMultTest(dut: PointMult, a: Int, p: Int,
                    px: Int, py: Int, s: Int,
                    rx: Int, ry: Int): Unit = {
    println("PM Tests starting: (" + px, py +") * (" + s + ") = (" + rx, ry +")")
    dut.io.a.poke(a.S)
    dut.io.p.poke(p.S)
    dut.io.px.poke(px.S)
    dut.io.py.poke(py.S)
    dut.io.s.poke(s.S)
    dut.io.load.poke(true.B)
    dut.clock.step()
    dut.io.load.poke(false.B)
    while ((dut.io.valid.peek().litValue() == 0)) dut.clock.step(1)
    dut.io.outx.expect(rx.S)
    dut.io.outy.expect(ry.S)
  }

  def PointMultWave(dut: PointMult, a: Int, p: Int,
                    px: Int, py: Int, s: Int): Unit = {
    dut.io.a.poke(a.S)
    dut.io.p.poke(p.S)
    dut.io.px.poke(px.S)
    dut.io.py.poke(py.S)
    dut.io.s.poke(s.S)
    dut.io.load.poke(true.B)
    dut.clock.step()
    dut.io.load.poke(false.B)
  }
}

/*
class WaveformTester(dut: PointMult) extends PeekPokeTester(dut) {
  val t = new testfunctions
  t.PointMultWave(dut, 0, 17, 15, 13, 2)
  dut.clock.step(10)
}

class WaveformSpec extends FlatSpec with Matchers {
  "Waveform" should "pass" in {
    Driver.execute(Array("--generate-vcd-output", "on"), () =>
        new PointMult(32,32)) { c =>
      new WaveformTester(c)
    } should be (true)
  }
}
*/

class MSMtest extends FreeSpec with ChiselScalatestTester {
  val t = new testfunctions()
  "PointMult Tests - manual tests" in {
    test (new PointMult(32, 32)) { dut =>
      //t.PointMultTest(dut, 0, 17, 15, 13, 1, 15, 13)
      t.PointMultTest(dut, 0, 17, 15, 13, 2,  2, 10)
      t.PointMultTest(dut, 0, 17, 15, 13, 3,  8,  3)
    }
  }

  "PointMult Tests - extensive tests" in {
    test (new PointMult(32, 32)) { dut =>
      val p1707 = new EllipticCurve(0, 7, 17)
      val g = new Point(15, 13, p1707) // generator point
      for (i <- 2 until 17)  {
        val r = g * i
        t.PointMultTest(dut, 0, 17, 15, 13, i, r.x, r.y)
      }
    }
  }

/*  "PointAddition Tests - manual tests" in {
    test (new PointAddition(32)) { dut =>
      t.PointAdditionTest(dut,  0, 17, 15, 13, 15, 13,  2, 10) // point double
      //t.PointAdditionTest(dut,  0, 17, 15, 13,  2, 10,  8,  3)
      //t.PointAdditionTest(dut,  0, 17,  0,  0,  6, 11,  6, 11) // p1 at inf
      //t.PointAdditionTest(dut,  0, 17, 15, 13,  0,  0, 15, 13) // p2 at inf
      //t.PointAdditionTest(dut,  0, 17,  3,  0,  6, 11, 12,  1)
    }
  }*/

/*  "PointAddition Tests - more extensive, utilize functional model" in {
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
  }*/



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