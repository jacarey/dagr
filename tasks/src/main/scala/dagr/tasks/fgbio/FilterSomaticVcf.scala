/*
 * The MIT License
 *
 * Copyright (c) 2017 Fulcrum Genomics LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package dagr.tasks.fgbio

import dagr.tasks.DagrDef.{PathToBam, PathToVcf}

import scala.collection.mutable.ListBuffer

case class FilterSomaticVcf(in: PathToVcf,
                            bam: PathToBam,
                            out: PathToVcf,
                            sample: Option[String] = None,
                            minMapQ: Option[Int] = None,
                            minBaseQuality: Option[Int] = None,
                            pairedReadsOnly: Option[Boolean] = None,
                            endRepairDistance: Option[Int] = None,
                            endRepairPValue: Option[Double] = None
                           ) extends FgBioTask {

  override protected def addFgBioArgs(buffer: ListBuffer[Any]): Unit = {
    buffer.append("-i", in)
    buffer.append("-b", bam)
    buffer.append("-o", out)
    sample.foreach(buffer.append("-s", _))
    minMapQ.foreach(buffer.append("-m", _))
    minBaseQuality.foreach(buffer.append("-q", _))
    pairedReadsOnly.foreach(buffer.append("-p", _))
    endRepairDistance.foreach(buffer.append("--end-repair-distance", _))
    endRepairPValue.foreach(buffer.append("--end-repair-p-value", _))
  }
}
