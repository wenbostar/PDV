args=commandArgs()

library(proteoQC)
qcres <- msQCpipe(spectralist = args(1),fasta = args(2),outdir = args(3),mode = args(4),miss = args(5),enzyme = args(6),
                  varmod = args(7), fixmod = args(8),tol = args(9),tolu = args(10),itol = args(11),itolu = args(12),
                  threshold = args(13),cpu = args(14),xmx = args(15),refine = args(16),ntt = args(17))
html <- reportHTML(qcres)
