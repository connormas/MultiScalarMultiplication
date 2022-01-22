package MSM

import chisel3._
import chisel3.util.RegEnable

/** TODO
 * - custom bundle interface for PointAddition
 * - custom point bundle
 * - latch values into regs in point addition
 * - is there a faster way to calculate mod inverse?
 * */


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


  // latch inputs into regs
  val p1x = RegEnable(io.p1x, 0.S, io.load)
  val p1y = RegEnable(io.p1y, 0.S, io.load)
  val p2x = RegEnable(io.p2x, 0.S, io.load)
  val p2y = RegEnable(io.p2y, 0.S, io.load)

  // instantiations
  val modinv = Module(new ModularInverse(w))
  val l = Wire(SInt())
  val new_x = Wire(SInt())
  val new_y = Wire(SInt())

  // control signals
  val inverses = p1x === p2x && p1y === -p2y
  val p1inf = p1x === 0.S && p1y === 0.S
  val p2inf = p2x === 0.S && p2y === 0.S

  // default values and assignments
  modinv.io.p := io.p
  modinv.io.load := RegNext(io.load) // delay signal by one cycle bc it takes a cycle to latch velues
  io.outx := 0.S
  io.outy := 0.S
  new_x := 0.S
  new_y := 0.S

  // create new point coordinates, when not dealing w special case
  when (!p1inf && !p2inf && !inverses) {
    new_x := ((l * l)  - p1x - p2x) % io.p
    io.outx := new_x
    when (new_x < 0.S) {
      io.outx := new_x + io.p
    }
    new_y := (l * (p1x - new_x) - p1y) % io.p
    io.outy := new_y
    when (new_y < 0.S) {
      io.outy := new_y + io.p
    }
  }
  
  // calculate lambda, handle case when P1 == P2
  modinv.io.a := p2x - p1x
  when (p1x === p2x && p1y === p2y) { // point double
    new_x := ((l * l)  - p1x - p1x) % io.p
    modinv.io.a := 2.S * p1y
    l := (3.S * p1x * p1x + io.a) * modinv.io.out
  } .otherwise {
    l := (p2y - p1y) * modinv.io.out
  }

  // assert valid signal, handles special cases
  io.valid := false.B
  when (modinv.io.valid && !io.load) {
    io.valid := true.B
  } .elsewhen (inverses) { // output point at infinity
    io.valid := true.B     // when P1 == -P2
    io.outx := 0.S
    io.outy := 0.S
  } .elsewhen (p1inf) {    // p1 is point at inf
    io.valid := true.B
    io.outx := p2x
    io.outy := p2y
  } .elsewhen (p2inf) {   // p2 is point at inf
    io.valid := true.B
    io.outx := p1x
    io.outy := p1y
  }

  // debugging
  //printf(p"--- inside PAdd (${io.outx},${io.outy}), modinvout=${modinv.io.out}, load=${io.load}, valid=${io.valid}, p1inf=${p1inf}, p2inf=${p2inf}\n\n")
  //printf(p"--- inside PAdd (${io.p1x},${io.p1y}) + (${io.p2x},${io.p2y}) = (${io.outx},${io.outy}), load=${io.load}, valid=${io.valid}\n\n")
}

/* hardware module that performs ec point additon. */
class PointAdditionB(val w: Int) extends Module {
  val io = IO(new Bundle {
    val a =     Input(SInt(w.W))
    val p =     Input(SInt(w.W))
    val p1 =    Input(new PointBundle(w))
    val p2 =    Input(new PointBundle(w))
    val load =  Input(Bool())
    val outx =  Output(SInt(w.W))
    val outy =  Output(SInt(w.W))
    val valid = Output(Bool())
  })

  // instantiations
  val modinv = Module(new ModularInverse(w))
  val l = Wire(SInt())
  val new_x = Wire(SInt())
  val new_y = Wire(SInt())

  // control signals
  val inverses = io.p1.x === io.p2.x && io.p1.y === -io.p2.y
  val p1inf = io.p1.x === 0.S && io.p1.y === 0.S
  val p2inf = io.p2.x === 0.S && io.p2.y === 0.S

  // default values and assignments
  modinv.io.p := io.p
  modinv.io.load := io.load
  io.outx := 0.S
  io.outy := 0.S
  new_x := 0.S
  new_y := 0.S

  // create new point coordinates, when not dealing w special case
  when (!p1inf && !p2inf && !inverses) {
    new_x := ((l * l)  - io.p1.x - io.p2.x) % io.p
    io.outx := new_x
    when (new_x < 0.S) {
      io.outx := new_x + io.p
    }
    new_y := (l * (io.p1.x - new_x) - io.p1.y) % io.p
    io.outy := new_y
    when (new_y < 0.S) {
      io.outy := new_y + io.p
    }
  }

  // calculate lambda, handle case when P1 == P2
  modinv.io.a := io.p2.x - io.p1.x
  when (io.p1.x === io.p2.x && io.p1.y === io.p2.y) { // point double
    new_x := ((l * l)  - io.p1.x - io.p1.x) % io.p
    modinv.io.a := 2.S * io.p1.y
    l := (3.S * io.p1.x * io.p1.x + io.a) * modinv.io.out
  } .otherwise {
    l := (io.p2.y - io.p1.y) * modinv.io.out
  }

  // assert valid signal, handles special cases
  io.valid := false.B
  when (modinv.io.valid) {
    io.valid := true.B
  } .elsewhen (inverses) { // output point at infinity
    io.valid := true.B     // when P1 == -P2
    io.outx := 0.S
    io.outy := 0.S
  } .elsewhen (p1inf) {    // p1 is point at inf
    io.valid := true.B
    io.outx := io.p2.x
    io.outy := io.p2.y
  } .elsewhen (p2inf) {   // p2 is point at inf
    io.valid := true.B
    io.outx := io.p1.x
    io.outy := io.p1.y
  }

  // debugging
  //printf(p"out = (${io.outx},${io.outy}), modinvout=${modinv.io.out}, valid=${io.valid}, p1inf=${p1inf}\n\n")
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
  //printf(p"a=${a}, p=${p}, n=${n}, valid=${io.valid}\n\n")
}
