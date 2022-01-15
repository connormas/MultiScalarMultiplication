package PippengerModel

object Util {
  // uses euclidean method to find greatest common denominator
  // between two numbers
  def gcd(a: Int, b: Int): Int = {
    if (b == 0) {
      return a
    } else {
      gcd(b, a % b)
    }
  }

  // finds the modular inverse of a mod(p)
  def mod_inverse(a: Int, p: Int): Int = {
    def mod_inv_recurse(a: Int, p: Int, n: Int): Int = {
      val r = (a * n) % p
      if (r != 1) {
        return mod_inv_recurse(a, p, n+1)
      }
      return n
    }
    if (a < 0) {
      return p - mod_inverse(-a, p)
    }
    mod_inv_recurse(a, p, 0)
  }

  // this performs bitwise multiplication. There is no need to actually
  // do multiplication this was in pure Scala, but it is a model of how
  // hardware could do this multiplication efficiently. This is just here
  // for personal reference.
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
}
