package MSM

import chisel3._

/** TODO
 * - make PointDouble module
 *   - separate module? mux calculation of l?
 * - custom bundle interface for PointAddition
 * - handle point at infinity
 * - latch values into regs in point addition
 * - add load/reset input into mod inverse
 * - is there a faster way to calculate mod inverse?
 * */

/* bundle that represents two coordinates in a point
 * not currently in use */
class PABundleInput(val w: Int) extends Bundle {
  val x = SInt(w.W)
  val y = SInt(w.W)
}

/* hardware module that performs ec point additon. */
class PointAddition(val w: Int) extends Module {
  val io = IO(new Bundle {
    val a =     Input(SInt(w.W))
    val p =     Input(SInt(w.W))
    val p1x =   Input(SInt(w.W))
    val p1y =   Input(SInt(w.W))
    val p2x =   Input(SInt(w.W))
    val p2y =   Input(SInt(w.W))
    val load =  Input(Bool())
    val outx =  Output(SInt(w.W))
    val outy =  Output(SInt(w.W))
    val valid = Output(Bool())
  })
  val modinv = Module(new ModularInverse(w))
  modinv.io.a := io.p2x - io.p1x
  modinv.io.p := io.p
  modinv.io.load := io.load
  io.outx := 0.S
  io.outy := 0.S
  io.valid := false.B
  val l = (io.p2y - io.p1y) * modinv.io.out

  // create new point coordinates set valid when finished
  val new_x = ((l * l)  - io.p1x - io.p2x) % io.p
  io.outx := new_x
  when (new_x < 0.S) {
    io.outx := new_x + io.p
  }
  val new_y = (l * (io.p1x - new_x) - io.p1y) % io.p
  io.outy := new_y
  when (new_y < 0.S) {
    io.outy := new_y + io.p
  }

  // assert valid signal
  when (modinv.io.valid) {
    io.valid := true.B
  }

  // debugging
  //printf(p"out = (${io.outx},${io.outy}), modinvout=${modinv.io.out}, valid=${io.valid}\n")// modinv.valid=${modinv.io.valid}\n")
}

/* finds the modular inverse of A mod P */
class ModularInverse(val w: Int) extends Module {
  val io = IO(new Bundle {
    val a =     Input(SInt(w.W))
    val p =     Input(SInt(w.W))
    val load =  Input(Bool())
    val valid = Output(Bool())
    val out =   Output(SInt(w.W))
  })

  val a = RegInit(0.S(w.W))
  val p = RegInit(0.S(w.W))
  val n = RegInit(0.S(w.W))
  val condition = (a * n) % p
  val neg = io.a < 0.S

  when (io.load) {
    a := Mux(neg, -io.a, io.a)
    p := io.p
    n := 0.S
    io.valid := false.B
  }

  io.out := Mux(neg, io.p - n, n)
  io.valid := false.B

  when (condition =/=  1.S && n < p) {
    n := n + 1.S
  } .elsewhen (condition === 1.S && n < p) {
    io.valid := true.B
  } .elsewhen (n === p) {
    io.out := 0.S
    io.valid := true.B
  }

  // use when debugging ModularInverse() only
  //printf(p"a=${a}, p=${p}, n=${n}, valid=${io.valid}\n")
}