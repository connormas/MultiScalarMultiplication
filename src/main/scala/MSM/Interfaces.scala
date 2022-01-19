package MSM

import chisel3._

/** TODO
 * - remove cloneType when upgrade to chisel3.5
 * */

/* bundle that represents affine (x,y) coordinate */
class Point(val w: Int) extends Bundle {
  val x = SInt(w.W)
  val y = SInt(w.W)
  override def cloneType = (new Point(w)).asInstanceOf[this.type]
}

/* bundle that represents projective coordinates where
 * x = X/X and y = Y/Z */
class PointProjective(val w: Int) extends Bundle {
  val x = SInt(w.W)
  val y = SInt(w.W)
  val z = SInt(w.W)
  override def cloneType = (new PointProjective(w)).asInstanceOf[this.type]
}
