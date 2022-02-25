# Multi-Scalar Multiplication in Chisel

This is a Chisel Module that performs Multi-Scalar Multiplication over Elliptic Curves.
This is also my Senior Thesis Project. Feel free to clone it, try running it, or make improvements.


### Let's go over what modules are here:
* Mod Inverse: Calculates a modular inverse.
* Point Addition: Calculates Elliptic Curve Point Addition. More info on how that works can be found here: https://crypto.stanford.edu/pbc/notes/elliptic/explicit.html 
* Point Multiplication: Calculates EC Point Multiplication by instantiating a PAdd module and performing repeated additions.
* Point Add Reduction: Takes in a Vector of EC points and adds them together with a PAdd module.
* Top Level MSM: Main MSM module. Takes in vectors of EC points and Scalars and computes a dot product between them resulting in a single EC Point.

