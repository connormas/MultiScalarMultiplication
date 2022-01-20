package MSM

import chisel3._

/** TODO
 * - remove cloneType when upgrade to chisel3.5
 * */

/* bundle that represents affine (x,y) coordinate */
class PointBundle(val w: Int) extends Bundle {
  val x = SInt(w.W)
  val y = SInt(w.W)

  def apply(X: Int, Y: Int): Unit = {
    x := X.S
    y := Y.S
  }
  override def cloneType = (new PointBundle(w)).asInstanceOf[this.type]
}

/* bundle that represents projective coordinates where
 * x = X/X and y = Y/Z */
class PointProjectiveBundle(val w: Int) extends Bundle {
  val x = SInt(w.W)
  val y = SInt(w.W)
  val z = SInt(w.W)
  override def cloneType = (new PointProjectiveBundle(w)).asInstanceOf[this.type]
}
