package MSM

/**
 * This is an elliptic curve class that tracks a curves
 * a and b coefficients, as well as the order of the
 * finite field it is defined over. The elliptic curve
 * will have the form: y^2 = A*x^3 + B*x (mod P)
 * param A: a coefficient
 * param B: a coefficient
 * param P: order of the finite field, hopefully prime
 * param G: generator point for this curve
 */
class EllipticCurve(A: BigInt, B: BigInt, P: BigInt) {
  val a: BigInt = A
  val b: BigInt = B
  val p: BigInt = P
  //val g: Point = G
}
