package MSM

object Util {
  // from github.com/alexmgr/tinyec/blob/master/tinyec/ec.py
  def gcd(a: BigInt, b: BigInt): (BigInt, BigInt, BigInt) = {
    if (a == 0) {
      return (b, 0, 1)
    } else {
      val (g, y, x) = gcd(b % a, a)
      return (g, (x - (b / a) * y), y)
    }
  }

  // from github.com/alexmgr/tinyec/blob/master/tinyec/ec.py
  def modinv(a: BigInt, p: BigInt): BigInt = {
    if (a < 0) {
      return p - modinv(-a, p)
    }
    val (g, x, y) = gcd(a, p)
    if (g == 1) {
      return x % p
    }
    return BigInt("-1")
  }

  /*
   * this performs bitwise multiplication. There is no need to actually do
   * multiplication this way in pure Scala, but it is a model of how hardware
   * could do this multiplication efficiently. This is just here for personal reference.
   */
  def bitwisemultiplication_Model(a: Int, b: Int): Int = {
    var result = 0
    for (i <- 0 until 32) {
      val operand = b << i
      val mask = a >> i
      if ((mask & 1) == 1) {
        result += operand
      }
    }
    result
  }

  /*
   * This mimics what the MSM part of zkSNARK does. It takes in two vectors.
   * One vectors of scalars, and another vector of points. It multiplies each
   * scalar by its corresponding point and them sums up all the points.
   */
  def zksnarkMSM_model(g: List[Point], e: List[Int]): Point = {
    assert(g.length == e.length, "vectors should be the same length")
    // multipy corresponding indexes of arrays and sum them up
    g zip e map { case (gi, ei) => gi * ei } reduce {_ + _}
  }
}
