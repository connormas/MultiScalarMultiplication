package PippengerModel

import Util._

/**
 * This class represents a point on an elliptic curve in a finite field.
 * It takes just two arguments, an X and Y coordinate. There are a number
 * of operations defined for these types of points.
 */
class Point(coord_x: Int, coord_y: Int, ec: EllipticCurve) {
  val x: Int = coord_x
  val y: Int = coord_y
  val curve: EllipticCurve = ec

  def double() = {
    val l = (3 * this.x * this.x + this.curve.a) * Util.mod_inverse(2 * this.y, this.curve.p)
    var new_x = ((l * l)  - this.x - this.x) % this.curve.p
    if (new_x < 0) {
      new_x = new_x + this.curve.p
    }
    var new_y = ((l * (this.x - new_x)) - this.y) % this.curve.p
    if (new_y < 0) {
      new_y = new_y + this.curve.p
    }
    new Point(new_x, new_y, this.curve)
  }

  def +(that: Point): Point = {
    if (this == that) {
      return this.double()
    }

    if (x == 0 && y == 0) {
      return that
    } else if (that.x == 0 && that.y == 0) {
      return this
    }


    val modinv = Util.mod_inverse(that.x - this.x, this.curve.p)
    if (modinv == -1) return new Point(0, 0, this.curve)
    val l = (that.y - this.y)  * modinv
    var new_x = ((l * l)  - this.x - that.x) % this.curve.p
    if (new_x < 0) {
      new_x = new_x + this.curve.p
    }
    var new_y = ((l * (this.x - new_x)) - this.y) % this.curve.p
    if (new_y < 0) {
      new_y = new_y + this.curve.p
    }
    new Point(new_x, new_y, this.curve)
  }

  def unary_- = new Point(this.x, this.y * -1, this.curve)

  def *(n: Int): Point = {
    if (n == 0) {
      return new Point(0, 0, this.curve)
    }
    def mult_helper(orig: Point, acc: Point, n: Int): Point = {
      if (n == 1) {
        return acc
      } else {
        val new_total = acc + orig
        mult_helper(orig, new_total, n-1)
      }
    }
    mult_helper(this, this, n)
  }

  def ==(that: Point) = {
    this.x == that.x && this.y == that.y
  }

  def print() = {
    println(s"I am the point (${this.x}, ${this.y})")
  }
}
