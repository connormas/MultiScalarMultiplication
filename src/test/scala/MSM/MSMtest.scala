package MSM

//import MSM.{EllipticCurve, Point, zksnarkMSM_model}
import Util._
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
    //println("PA Tests starting: (" + p1x, p1y +") + (" + p2x, p2y + ") = (" + rx, ry +")")
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
    //dut.clock.step(3)
    dut.io.outy.expect(ry.S)
    dut.io.outx.expect(rx.S)
    dut.clock.step()
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

  def betterReductionTest(dut: PAddReduction, points: Seq[(Int, Int)],
                          a: BigInt, b: BigInt, p: BigInt): Unit = {

    // make curve and list of Point objects
    val curve = new EllipticCurve(a, b, p)
    val refpoints = points map { case(x,y) => new Point(x, y, curve) }
    val result = refpoints reduce {_ + _}

    points.zipWithIndex foreach { case ((x, y), i) =>
      dut.io.xs(i).poke(x.S)
      dut.io.ys(i).poke(y.S)
    }
    dut.io.load.poke(true.B)
    dut.clock.step()
    dut.io.load.poke(false.B)
    while ((dut.io.valid.peek().litValue() == 0)) dut.clock.step(1)
    dut.clock.step(5)
    //dut.io.outx.expect(result.x.S)
    //dut.io.outy.expect(result.y.S)
    //dut.clock.step()
  }


  def TopLevelTest(dut: TopLevelMSM, x: Seq[SInt], y: Seq[SInt], s: Seq[SInt],
                  rx: Int, ry: Int): Unit = {
    println(s"TopLevelTest nummults = ${x.length}")
    x.zipWithIndex foreach { case (x, i) => dut.io.pointsx(i).poke(x) }
    y.zipWithIndex foreach { case (y, i) => dut.io.pointsy(i).poke(y) }
    s.zipWithIndex foreach { case (s, i) => dut.io.scalars(i).poke(s) }
    dut.io.load.poke(true.B)
    dut.io.complete.poke(true.B)
    dut.clock.step()
    dut.io.load.poke(false.B)
    while ((dut.io.valid.peek().litValue() == 0)) dut.clock.step(1)
    dut.io.outx.expect(rx.S)
    dut.io.outy.expect(ry.S)
    dut.clock.step(3)
  }

  def TopLevelTestVariableLength(dut: TopLevelMSM, points: Seq[(Int, Int)], scalars: Seq[Int],
                                 rs: Int, wkld: Int, a: Int, b: Int, p: Int): Unit = {
    assert (points.size == scalars.size)
    //assert (points.size % rs == 0)
    println(s"TopLevelTest wkld: ${wkld} size: ${rs}")


    var allinputs = points zip scalars
    allinputs = (0 until 1000) flatMap { i => allinputs }
    val numInputs = allinputs.size / rs


    //allinputs foreach println

    val curve = new EllipticCurve(a, b, p)

    for (iter <- 0 until numInputs) {
      //println(s"iteration $iter")
      val currentinputs = allinputs.take(rs)
      //currentinputs foreach { case ((x,y), s) => print(s"(${x},${y}) * ${s} + ")}
      //println("\n")
      allinputs = allinputs.drop(rs)
      currentinputs.zipWithIndex foreach { case(((x,y), s), i) => // point zipped with scalar zipped with ind
        dut.io.pointsx(i).poke(x.S)
        dut.io.pointsy(i).poke(y.S)
        dut.io.scalars(i).poke(s.S)
      }
      dut.io.load.poke(true.B)
      dut.clock.step()
      dut.io.load.poke(false.B)
      while ((dut.io.valid.peek().litValue() == 0)) dut.clock.step(1)

      // expect outputs
      val currPoints = currentinputs map { case ((x,y), s) => new Point(x, y, curve)}
      val currScalars = currentinputs map { case ((x,y), s) => s}
      val result = zksnarkMSM_model(currPoints, currScalars)
      //result.print()
      dut.io.outx.expect(result.x.S)
      dut.io.outy.expect(result.y.S)
      dut.clock.step(2)
    }
  }
}


class MSMtest extends FreeSpec with ChiselScalatestTester {
  val t = new testfunctions()

  "TopLevelMSM Tests (size 4) - manual tests" in {
    test(new TopLevelMSM(8, 8, 0, 17, 4, 4)) { dut =>
      val xs = Seq(1, 5, 6, 12) map (x => x.S(8.W))
      val ys = Seq(5, 9, 6, 1) map (y => y.S(8.W))
      val ss = Seq(2, 4, 6, 8) map (y => y.S(8.W))
      t.TopLevelTest(dut, xs, ys, ss, 12, 1)
      //val xs = Seq(15, 2, 8, 12) map (x => x.S(8.W))
      //val ys = Seq(13,10, 3, 1) map (y => y.S(8.W))
      //val ss = Seq( 1, 2, 3, 4) map (s => s.S(8.W))
      //t.TopLevelTest(dut, xs, ys, ss, 5, 9)
      //val xs1 = Seq(15, 2, 8, 12) map (x => x.S(8.W))
      //val ys1 = Seq(13,10, 3, 1) map (y => y.S(8.W))
      //val ss1 = Seq( 1, 2, 3, 4) map (s => s.S(8.W))
      //t.TopLevelTest(dut, xs1, ys1, ss1, 5, 9)
      //val xs2 = Seq(6, 1,10, 15) map (x => x.S(8.W))
      //val ys2 = Seq(6, 5, 2, 13) map (y => y.S(8.W))
      //val ss2 = Seq( 9,10,11,12) map (s => s.S(8.W))
      //t.TopLevelTest(dut, xs2, ys2, ss2, 1, 12)
    }
  }

  /*"TopLevelMSM Tests (size 8) - manual tests" in {
    test(new TopLevelMSM(8, 8, 0, 17, 8, 8)) { dut =>
      val xs = Seq(1, 5, 6, 12, 5, 2, 1, 10) map (x => x.S(8.W))
      val ys = Seq(5, 9, 6, 1, 8, 7, 12, 15) map (y => y.S(8.W))
      val ss = Seq(2, 4, 6, 8, 1, 3, 5, 7) map (y => y.S(8.W))
      t.TopLevelTest(dut, xs, ys, ss, 8, 3)
      //t.TopLevelTest(dut, xs, ys, ss, 8, 3) // do it again?
    }
  }*/

  // Much more extensive top level tests
  // WARNING: Very time consuming!
  val testSizes = Seq(2,4,6,12,16,24)
  tastSizes foreach { s =>
    s"TopLevelMSM W 1 size ${s} function" in {
      val size = s
      test (new TopLevelMSM(8,8,0,17,size,size)) { dut =>
        val points = Seq((15,13), (2,10), (8,3), (12,1), (6,6), (5,8), (10,15), (1,12), (6,6), (1,5), (10,2), (15,13))
        val scalars = Seq(1,2,3,4,5,6,7,8,9,10,11,12)
        t.TopLevelTestVariableLength(dut, points, scalars, size, 1, 0, 7, 17)
      }
    }
    s"TopLevelMSM W 2 size ${s} function" in {
      val size = s
      test (new TopLevelMSM(8,8,0,17,size,size)) { dut =>
        val points2 = Seq((15,13), (2,10), (8,3), (12,1), (6,6), (6,6), (10,15), (1,12), (6,6), (1,5), (10,2), (15,13))
        val scalars2 = Seq(1,20,2,4, 19,6,7,12, 22,1,11,12)
        t.TopLevelTestVariableLength(dut, points2, scalars2, size, 2, 0, 7, 17)
      }
    }
  }


/*"PAdd Reduction Tests (size 4) - manual tests" in {
  test (new PAddReduction2(4, 1size, 0, 17)) { dut =>
    //val xs0 = Seq( 1,  1,  8, 12) map (x => x.S(8.W))
    //val ys0 = Seq( 5, 12,  3,  1) map (y => y.S(8.W))
    //t.PAReductionTest(dut, xs0, ys0, 10, 15)
    //val xs1 = Seq( 1,  3,  8, 12) map (x => x.S(8.W))
    //val ys1 = Seq( 5,  0,  3,  1) map (y => y.S(8.W))
    //t.PAReductionTest(dut, xs1, ys1, 1, 12)
    //val xs2 = Seq( 2, 12,  0, 12) map (x => x.S(8.W))
    //val ys2 = Seq(10, 16,  0, 16) map (y => y.S(8.W))
    //t.PAReductionTest(dut, xs2, ys2, 5, 9)
    val xs2 = Seq( 3, 1,  6, 1) map (x => x.S(8.W)) // last point is inf
    dut.clock.step(20)
    val ys2 = Seq( 0, 5, 11, 5) map (y => y.S(8.W))
    t.PAReductionTest(dut, xs2, ys2, 5, 8)
  }
}*/

/*"Reduction Tests (size 4) - manual, better function" in {
  test (new PAddReduction2(4, 16, 0, 17)) { dut =>
    //val points3 = Seq((6,6),(12,1),(5,9),(2,7))
    //t.betterReductionTest(dut, points3, 0, 7, 17)
    //val points2 = Seq((15,13),(12,1),(3,0),(2,7))
    //t.betterReductionTest(dut, points2, 0, 7, 17)
    //val points = Seq((10,15),(0,0),(6,11),(1,5)) // gets no modinv error?
    //t.betterReductionTest(dut, points, 0, 7, 17)
    //val points1 = Seq((3,0), (1,5), (6,11), (0,0))
    //t.betterReductionTest(dut, points1, 0, 7, 17)
    //val points1 = Seq((6,6), (0,0), (6,11), (5,8)) // this needs fixing
    //t.betterReductionTest(dut, points1, 0, 7, 17)
  }
}*/

/*"PAdd Reduction Tests (size 17) - manual tests" in {
  test (new PAddReduction2(17, 16, 0, 17)) { dut =>
    val xs1 = Seq(15,  2,  8, 12,  6,  5, 10,  1,  3,  1, 10,  5,  6, 12,  8,  2, 15) map (x => x.S(8.W))
    val ys1 = Seq(13, 10,  3,  1,  6,  8, 15, 12,  0,  5,  2,  9, 11, 16, 14,  7,  4) map (y => y.S(8.W))
    t.PAReductionTest(dut, xs1, ys1, 3, 0)
  }
}*/

/*"PMBitSerial Tests - manual tests" in {
  test (new PMBitSerial(8, 8)) { dut =>
    dut.io.a.poke(0.S)
    dut.io.p.poke(17.S)
    dut.io.px.poke(15.S)
    dut.io.py.poke(13.S)
    dut.io.s.poke(15.S)
    dut.io.load.poke(true.B)
    dut.clock.step()
    dut.io.load.poke(false.B)
    while ((dut.io.valid.peek().litValue() == 0)) dut.clock.step(1)
  }
}*/

/*"PointMultNaive Tests - manual tests" in {
test (new PMNaive(16, 16)) { dut =>
  t.PMNaiveTest(dut, 0, 17, 15, 13, 0,  0,  0)  // mult by zero
  t.PMNaiveTest(dut, 0, 17, 15, 13, 1, 15, 13)  // mult by 1
  t.PMNaiveTest(dut, 0, 17, 15, 13, 2,  2, 10)
  t.PMNaiveTest(dut, 0, 17, 15, 13, 3,  8,  3)
  t.PMNaiveTest(dut, 0, 17, 15, 13, 18,  0, 0) // back to point at inf
  t.PMNaiveTest(dut, 0, 17, 15, 13, 17,  15, 4) // back to point at inf
  t.PMNaiveTest(dut, 0, 17, 15, 13, 19,  15, 13) // back to point at inf
  t.PMNaiveTest(dut, 0, 17, 12, 16, 8,  12, 1)
  t.PMNaiveTest(dut, 0, 17, 12, 16, 9, 0, 0)
  t.PMNaiveTest(dut, 0, 17, 12, 16, 8, 12, 1) // this shit copied
  t.PMNaiveTest(dut, 0, 17, 12, 16, 9, 0, 0) // this shit copied
  t.PMNaiveTest(dut, 0, 17, 12, 16, 10, 12, 16) // this shit
  //t.PMNaiveTest(dut, 0, 17, 12, 16, 11, 1, 5)
  //t.PMNaiveTest(dut, 0, 17, 12, 16, 29, 1, 5)
  //t.PMNaiveTest(dut, 0, 17, 12, 16, 34,  1, 12)
  //t.PMNaiveTest(dut, 0, 17, 12, 16, 35,  12, 1)
  //t.PMNaiveTest(dut, 0, 17, 1, 12, 6, 5, 9)
  //t.PMNaiveTest(dut, 0, 17, 12, 1, 4, 2, 7)
  //t.PMNaiveTest(dut, 0, 17, 15, 13, 21, 8, 3)
  t.PMNaiveTest(dut, 0, 17, 2, 10, 20, 12,1)
}
}*/

/*"PointMultNaive Tests - extensive tests" in {
test (new PMNaive(32, 32)) { dut =>
  val p1707 = new EllipticCurve(0, 7, 17)
  val g = new Point(15, 13, p1707) // generator point
  for (i <- 0 until 18)  {
    val r = g * i
    t.PMNaiveTest(dut, 0, 17, 15, 13, i, r.x, r.y)
  }
}
}*/


/*"PointAddition Tests - manual tests" in {
test (new PointAddition(32)) { dut =>
  t.PointAdditionTest(dut,  0, 17, 15, 13, 15, 13,  2, 10) // point double
  t.PointAdditionTest(dut,  0, 17, 15, 13,  2, 10,  8,  3)
  t.PointAdditionTest(dut,  0, 17,  0,  0,  6, 11,  6, 11) // p1 at inf
  t.PointAdditionTest(dut,  0, 17, 15, 13,  0,  0, 15, 13) // p2 at inf
  t.PointAdditionTest(dut,  0, 17,  3,  0,  6, 11, 12,  1)
  t.PointAdditionTest(dut,  0, 17, 15, 13, 15, 14,  0,  0)   //get back to point at inf
  t.PointAdditionTest(dut,  0, 17, 2, 10, 12, 16,  2,  7)   //get back to point at inf
  t.PointAdditionTest(dut,  0, 17, 2, 7, 0, 0,  2,  7)   //get back to point at inf
  t.PointAdditionTest(dut,  0, 17, 2, 7, 12, 16,  5, 9)   //get back to point at inf
  t.PointAdditionTest(dut,  0, 17, 15, 13,  15, 4,  0, 0)
  t.PointAdditionTest(dut,  0, 17, 1, 5,  1, 12,  0, 0)
}
}*/

/*"PointAddition Tests - more extensive, small curve" in {
test (new PointAddition(32)) { dut =>
  val p1707 = new EllipticCurve(0, 7, 17)
  val g = new Point(15, 13, p1707) // generator point

  for (i <- 0 until 100) {
    val t = g * i
    val r = g + t
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

/*"PointAddition Tests - slightly bigger curve" in {
test(new PointAddition(32)) { dut =>
  val p99707a = BigInt("-1")
  val p99707b = BigInt("1")
  val p99707p = BigInt("97")
  val p99707 = new EllipticCurve(p99707a, p99707b, p99707p)
  val xc = BigInt("76")
  val yc = BigInt("48")
  val gp = new Point(xc, yc, p99707)
  dut.io.a.poke(gp.curve.a.S)
  dut.io.p.poke(gp.curve.p.S)
  dut.io.p1x.poke(gp.x.S)
  dut.io.p1y.poke(gp.y.S)

  for (n <- 1 until 100) {
    val t = gp * n
    val r = gp + t
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
