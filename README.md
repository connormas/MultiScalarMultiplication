# Multi-Scalar Multiplication in Chisel

- TODO
  - custom Bundle for Point data. whether it be cartesian or projective, this is objectively cleaner
  - bucket dispatch logic to deal with bucket workload imbalance
  - data streaming interface (AXI, DMA, ???)
    - how much data is requested from main mem at a time?
    - is it streamed in or latched into regs?