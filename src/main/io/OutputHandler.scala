package edu.gsu.cs.kgem.io

import edu.gsu.cs.kgem.model.{Genotype, Read}
import java.io.{File, PrintStream}
import org.biojava3.core.sequence.DNASequence
import org.biojava3.core.sequence.io.FastaWriterHelper
import collection.JavaConversions._

/**
 * Created with IntelliJ IDEA.
 * User: aartyomenko
 * Date: 3/30/13
 * Time: 3:23 PM
 */
object OutputHandler {
  /**
   * Output corrected reads into specified {@see PrintStream}
   * @param out
   *            {@see PrintStream} object, either file or stdout
   * @param gens
   *             Collection of haplotypes (Result)
   */
  def outputResult(out: PrintStream, gens: Iterable[Genotype]) = {
    val gg = gens.map(g => (g.toIntegralString, g)).toMap
    for (g <- gg) {
      out.println(">read_freq=%.10f\n%s".format(g._2.freq, g._1))
    }
    out.close()
  }

  /**
   * Output corrected reads into specified {@see PrintStream}
   * @param out
   *            {@see PrintStream} object, either file or stdout
   * @param gens
   *             Collection of haplotypes (Result)
   * @param n
   *          Number of reads
   */
  def outputResult(out: PrintStream, gens: Iterable[Genotype], n: Int, clean:(String => String) = (s => s)) = {
    val gg = gens.toIndexedSeq.sortBy(g => -g.freq)
    val haplSeqs = gg.map(g => {
      val fn = (g.freq * n).asInstanceOf[Int]
      val cleanedSeq = clean(g.toIntegralString)
      List.fill(fn)((cleanedSeq, g.freq))
    }).flatten.zipWithIndex.map(g => {
      val dna = new DNASequence(g._1._1)
      dna.setOriginalHeader("read%d_freq_%.10f".format(g._2,g._1._2))
      dna
    })
    writeFasta(out, haplSeqs)
  }

  /**
   * Output haplotypes into specified {@see PrintStream}
   * @param out
   *            {@see PrintStream} object, either file or stdout
   * @param gens
   *             Collection of haplotypes (Result)
   */
  def outputHaplotypes(out: PrintStream, gens: Iterable[Genotype], clean:(String => String) = (s => s)) = {
    val gg = gens.toIndexedSeq.sortBy(g => -g.freq)
    val haplSeqs = gg.map(g => {
      val seq = new DNASequence(clean(g.toIntegralString))
      seq.setOriginalHeader("haplotype%d_freq_%.10f".format(g.ID,g.freq))
      seq
    })
    writeFasta(out, haplSeqs)
  }

  /**
   * Output reads with clustering info
   * @param out
   *             {@see PrintStream} object, either file or stdout
   * @param gens
   *             Collection of Haplotypes
   * @param reads
   *              Collection of reads
   * @param pqrs
   *             Clustering coefficients
   */
  def outputClusteredFasta(out: PrintStream, gens: Iterable[Genotype], reads: Iterable[Read], pqrs: Array[Array[Double]]) = {
    val ggs = gens.zipWithIndex

    val faReads = reads.zipWithIndex.map( rd => {
      val seq = new DNASequence(rd._1.seq.replace(" ", "").replace("-",""))
      seq.setOriginalHeader("read%d_%s".format(rd._2, clusteringString(ggs, pqrs, rd._2)))
      seq
    })

    writeFasta(out, faReads)
  }


  private def writeFasta(out: PrintStream, seq: Iterable[DNASequence]) {
    try {
      FastaWriterHelper.writeNucleotideSequence(out, seq)
    } finally {
      out.close()
    }
  }

  private def clusteringString(ggs: Iterable[(Genotype, Int)], pqrs: Array[Array[Double]], readIndex: Int) = {
    val sb = new StringBuffer()
    for (g <- ggs) {
      sb.append("_h%d=%.5f".format(g._1.ID, pqrs(g._2)(readIndex)))
    }
    sb.toString
  }

  /**
   * Setup the output directory and files.
   * @param dir
   *         The output directory file. This is where the reconstructed
   *         haplotypes and corrected reads will be stored.
   * @return
   *         Returns None if the output directory, or output files cannot
   *         be created. If they are created successfully then it returns
   *         Some((hapOutput: PrintStream, resultsOutput: PrintStream)).
   *
   */
  def setupOutputDir(dir: File): Option[(PrintStream, PrintStream, PrintStream, PrintStream, PrintStream)] = {
    // Try to make the output directory. If it fails, return None.
    if (!dir.exists()) {
      if (!dir.mkdir()) {
        println("Cannot create output directory!")
        return None
      }
    }

    // Try to open output files. If they fail, return None.
    val baseName = dir.getAbsolutePath() + File.separator
    val hapOutputName = "%s%s".format(baseName, "haplotypes.fa")
    val cleanedHapOutputName = "%s%s".format(baseName, "haplotypes_cleaned.fa")
    val readsOutputName = "%s%s".format(baseName, "reads.fa")
    val cleanedReadsOutputName = "%s%s".format(baseName, "reads_cleaned.fa")
    val readsClustered = "%s%s".format(baseName, "reads_clustered.fa")

    try {
        val hapout = new PrintStream(hapOutputName)
        try {
          val readsout = new PrintStream(readsOutputName)
          try {
            val hapclout = new PrintStream(cleanedHapOutputName)
            try {
              val readclsout = new PrintStream(cleanedReadsOutputName)
              try {
                val readsclust = new PrintStream(readsClustered)
                return Some((hapout, hapclout, readsout, readclsout, readsclust))
              } catch {
                case _: Throwable => println("Cannot create file: " + readsClustered); return None
              }
            } catch {
              case _: Throwable => println("Cannot create file: " + cleanedReadsOutputName); return None
            }
          } catch {
            case _: Throwable => println("Cannot create file: " + cleanedHapOutputName); return None
          }
        } catch {
          case _: Throwable => println("Cannot create file: " + readsOutputName); return None
        }
    } catch {
      case _: Throwable => println("Cannot create file: " + hapOutputName); return None
    }
  }
}