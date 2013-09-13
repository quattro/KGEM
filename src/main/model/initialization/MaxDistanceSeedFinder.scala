package edu.gsu.cs.kgem.model.initialization

import edu.gsu.cs.kgem.model.{Genotype, Read}
import java.util.Random
import scala.collection.mutable

/**
 * Created with IntelliJ IDEA.
 * User: aartyomenko
 * Date: 4/13/13
 * Time: 1:43 PM
 * Class to wrap derandomized KGEM, i. e. initialization with maximizing distance between
 * seeds and detection of size of the population via distance threshold.
 */
object MaxDistanceSeedFinder extends SeedFinder {

  /**
   * Find seeds according to maximization Hamming distance between all pairs
   * of reads with pre-specified threshold. This is a 2-approximation to the
   * metric k-center problem.
   *
   * @param reads
   * Collection of reads
   * @param k
   * Maximum size of sample
   * @param threshold
   * Min hamming distance between seeds
   * @return
   */
  def findSeeds(reads: Iterable[Read], k: Int, threshold: Int): Iterable[Genotype] = {
    val readArr = reads.toArray
    val first = getFirstSeed(readArr)
    var seeds = new mutable.MutableList[Read]()
    seeds += first
    var distanceMap = readArr.filter(r => !r.equals(first)).map(r => (r, hammingDistance(first, r)))
    var maxHD = 0
    while (seeds.size < k) {
      if (distanceMap.isEmpty) return seeds.map(r => new Genotype(r.seq))
      val cur = distanceMap.maxBy(e => e._2)
      maxHD = cur._2
      if (cur._2 < threshold) return seeds.map(r => new Genotype(r.seq))
      seeds += cur._1
      distanceMap = distanceMap.map(e => (e._1, min(e._2, hammingDistance(cur._1, e._1))))
    }
    println("Final max HD: %d".format(maxHD))
    return seeds.map(r => new Genotype(r.seq))
  }

  /**
   * Select first read randomly
   * @param readArr
   * Array with all reads
   * @return
   * one read
   */
  private def getFirstSeed(readArr: Array[Read]) = {
    val s = readArr.size
    val rnd = new Random()
    readArr(rnd.nextInt(s))
  }

  @inline
  def min(i1: Int, i2: Int): Int = {
    if (i1 < i2) return i1
    i2
  }

  /**
   * Wrapper for hamming distance between reads
   * @param r1
   * Read 1
   * @param r2
   * Read 2
   * @return
   * Hamming Distance between reads
   */
  @inline
  private def hammingDistance(r1: Read, r2: Read): Int = {
    if (r1.equals(r2)) return 0
    hammingDistance(r1.seq, r2.seq)
  }

  /**
   * Compute hamming distance between two strings
   * of the same length
   * @param s
   * String 1
   * @param t
   * String 2
   * @return
   * Hamming distance between s and t if
   * their length is the same and -1
   * otherwise
   */
  @inline
  def hammingDistance(s: String, t: String): Int = {
    val l = s.length
    if (l != t.length) {
      throw new IllegalArgumentException("Hamming Distance: Strings have different length")
    }
    var r = 0
    for (i <- 0 until l) {
      if (s(i) != t(i) && s(i) != ' ' && t(i) != ' ' && t(i) != '-' && s(i) != '-') {
        r += 1
      }
    }
    return r
  }
}