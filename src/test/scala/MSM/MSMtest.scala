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
  }

  def PMNaiveTest(dut: PMNaive, a: Int, p: Int,
                    px: Int, py: Int, s: Int,
                    rx: Int, ry: Int): Unit = {
    println("PMNaive Tests starting: (" + px, py +") * (" + s + ") = (" + rx, ry +")")
    dut.io.a.poke(a.S)
    dut.io.p.poke(p.S)
    dut.io.px.poke(px.S)
    dut.io.py.poke(py.S)
    dut.io.s.poke(s.S)
    dut.io.load.poke(true.B)
    dut.clock.step()
    dut.io.load.poke(false.B)
    while ((dut.io.valid.peek().litValue() == 0)) dut.clock.step(1)
    dut.io.outy.expect(ry.S)
    dut.io.outx.expect(rx.S)
  }

  def PMBSTest(dut: PMBitSerial, a: Int, p: Int,
                  px: Int, py: Int, s: Int,
                  rx: Int, ry: Int): Unit = {
    println("PMBS Tests starting: (" + px, py +") * (" + s + ") = (" + rx, ry +")")
    dut.io.a.poke(a.S)
    dut.io.p.poke(p.S)
    dut.io.px.poke(px.S)
    dut.io.py.poke(py.S)
    dut.io.s.poke(s.S)
    dut.io.load.poke(true.B)
    dut.clock.step()
    dut.io.load.poke(false.B)
    while ((dut.io.valid.peek().litValue() == 0)) dut.clock.step(1)
    dut.io.outy.expect(ry.S)
    dut.io.outx.expect(rx.S)
  }

  def PAReductionTest(dut: PAddReduction, x: Seq[SInt], y: Seq[SInt],
                      rx: Int, ry: Int): Unit = {
    x.zipWithIndex foreach { case (s, i) => dut.io.xs(i).poke(s) }
    y.zipWithIndex foreach { case (s, i) => dut.io.ys(i).poke(s) }
    dut.io.load.poke(true.B)
    dut.clock.step()
    dut.io.load.poke(false.B)
    while ((dut.io.valid.peek().litValue() == 0)) dut.clock.step(1)
    dut.io.outx.expect(rx.S)
    dut.io.outy.expect(ry.S)
    dut.clock.step()
  }

  def TopLevelTest(dut: TopLevelMSM, x: Seq[SInt], y: Seq[SInt], s: Seq[SInt],
                  rx: Int, ry: Int): Unit = {
    x.zipWithIndex foreach { case (x, i) => dut.io.pointsx(i).poke(x) }
    y.zipWithIndex foreach { case (y, i) => dut.io.pointsy(i).poke(y) }
    s.zipWithIndex foreach { case (s, i) => dut.io.scalars(i).poke(s) }
    dut.io.load.poke(true.B)
    dut.clock.step()
    dut.io.load.poke(false.B)
    while ((dut.io.valid.peek().litValue() == 0)) dut.clock.step(1)
    dut.io.outx.expect(rx.S)
    dut.io.outy.expect(ry.S)
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

  "TopLevelMSM Tests (size 4) - manual tests" in {
    test (new TopLevelMSM(16, 16, 0, 17, 4, 4)) { dut =>
      val xs = Seq( 1,  1,  8, 12) map (x => x.S(8.W))
      val ys = Seq( 5, 12,  3,  1) map (y => y.S(8.W))
      val ss = Seq( 2, 4,  6,  8) map (y => y.S(8.W))
      t.TopLevelTest(dut, xs, ys, ss, 5, 9)
    }
  }

  /*"TopLevelMSM Tests (size 8) - manual tests" in {
    test (new TopLevelMSM(16, 16, 0, 17, 8, 8)) { dut =>
      val points = Seq(Seq(15,13),Seq(8,3),Seq(1,5),Seq(12,16),
                       Seq(2,10),Seq(6,6),Seq(1,12),Seq(5,9),
                       Seq(2,7),Seq(12,1),Seq(1,12),Seq(6,6),
                       Seq(8,3),Seq(2,10),Seq(8,14),Seq(10,2))
      val scalars = Seq(4,5,6,34, 2,5,6,7, 8,4,2,1, 3,23,5,75)
      val xs = points map { case Seq(x, y) => x }
      val ys = points map { case Seq(x, y) => y }
      //t.TopLevelTest(dut, xs, ys, scalars, )
    }
  }*/

  "PAdd Reduction Tests (size 4) - manual tests" in {
    test (new PAddReduction(4, 16, 0, 17)) { dut =>
      val xs0 = Seq( 1,  1,  8, 12) map (x => x.S(8.W))
      val ys0 = Seq( 5, 12,  3,  1) map (y => y.S(8.W))
      t.PAReductionTest(dut, xs0, ys0, 10, 15)
      val xs1 = Seq( 1,  3,  8, 12) map (x => x.S(8.W))
      val ys1 = Seq( 5,  0,  3,  1) map (y => y.S(8.W))
      t.PAReductionTest(dut, xs1, ys1, 1, 12)
      val xs2 = Seq( 2, 12,  0, 12) map (x => x.S(8.W))
      val ys2 = Seq(10, 16,  0, 16) map (y => y.S(8.W))
      t.PAReductionTest(dut, xs2, ys2, 5, 9)
    }
  }

  "PAdd Reduction Tests (size 17) - manual tests" in {
    test (new PAddReduction(17, 16, 0, 17)) { dut =>
      val xs1 = Seq(15,  2,  8, 12,  6,  5, 10,  1,  3,  1, 10,  5,  6, 12,  8,  2, 15) map (x => x.S(8.W))
      val ys1 = Seq(13, 10,  3,  1,  6,  8, 15, 12,  0,  5,  2,  9, 11, 16, 14,  7,  4) map (y => y.S(8.W))
      t.PAReductionTest(dut, xs1, ys1, 3, 0)
    }
  }

/*  "PointMultNaive Tests - manual tests" in {
    test (new PMNaive(32, 32)) { dut =>
      t.PMNaiveTest(dut, 0, 17, 15, 13, 0,  0,  0)
      t.PMNaiveTest(dut, 0, 17, 15, 13, 1, 15, 13)
      t.PMNaiveTest(dut, 0, 17, 15, 13, 2,  2, 10)
      t.PMNaiveTest(dut, 0, 17, 15, 13, 3,  8,  3)
      t.PMNaiveTest(dut, 0, 17, 15, 13, 18,  0, 0)
    }
  }

  "PointMultNaive Tests - extensive tests" in {
    test (new PMNaive(32, 32)) { dut =>
      val p1707 = new EllipticCurve(0, 7, 17)
      val g = new Point(15, 13, p1707) // generator point
      for (i <- 0 until 18)  {
        val r = g * i
        t.PMNaiveTest(dut, 0, 17, 15, 13, i, r.x, r.y)
      }
    }
  }*/

  /*"PointMultBitSerial Tests - manual tests" in {
    test (new PMBitSerial(8, 8)) { dut =>
      t.PMBSTest(dut, 0, 17, 15, 13, 5,  2, 10)
      dut.clock.step(5)
    }
  }*/

  "PointAddition Tests - manual tests" in {
    test (new PointAddition(32)) { dut =>
      //t.PointAdditionTest(dut,  0, 17, 15, 13, 15, 13,  2, 10) // point double
      //t.PointAdditionTest(dut,  0, 17, 15, 13,  2, 10,  8,  3)
      //t.PointAdditionTest(dut,  0, 17,  0,  0,  6, 11,  6, 11) // p1 at inf
      //t.PointAdditionTest(dut,  0, 17, 15, 13,  0,  0, 15, 13) // p2 at inf
      //t.PointAdditionTest(dut,  0, 17,  3,  0,  6, 11, 12,  1)
      //t.PointAdditionTest(dut,  0, 17, 15, 13, 15, 14,  0,  0)   //get back to point at inf
      //t.PointAdditionTest(dut,  0, 17, 2, 10, 12, 16,  2,  7)   //get back to point at inf
      //t.PointAdditionTest(dut,  0, 17, 2, 7, 0, 0,  2,  7)   //get back to point at inf
      //t.PointAdditionTest(dut,  0, 17, 2, 7, 12, 16,  5, 9)   //get back to point at inf
    }
  }

/*  "PointAddition Tests - more extensive, utilize functional model" in {
    test (new PointAddition(32)) { dut =>
      val p1707 = new EllipticCurve(0, 7, 17)
      val g = new Point(15, 13, p1707) // generator point
      val limit = 50
      for (i <- 0 until limit) {
        val t = g * i
        val r = g + t
        if (limit < 20)
          println("PA Tests starting: (" + g.x, g.y +") + (" + t.x, t.y + ") = (" + r.x, r.y +")")
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
        if (i < 17) {
          dut.io.outx.expect(r.x.S)
          dut.io.outy.expect(r.y.S)
        }
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