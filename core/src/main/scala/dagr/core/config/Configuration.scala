/*
 * The MIT License
 *
 * Copyright (c) 2016 Fulcrum Genomics LLC
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
package dagr.core.config

import java.io.File
import java.nio.file.{Files, Path, Paths}
import java.time.Duration

import com.typesafe.config.ConfigException.Generic
import dagr.core.execsystem.{Cores, Memory}
import dagr.core.util.{Io, LazyLogging}

import scala.reflect.runtime.universe._

/**
  * Companion object to the Configuration trait that keeps track of all configuration keys
  * that have been requested so that they can be reported later if desired.
  */
private[core] object Configuration {
  private val RequestedKeys = collection.mutable.TreeSet[String]()

  /** Returns a sorted set of all keys that have been requested up to this point in time. */
  def requestedKeys : Set[String] = {
    var keys = collection.immutable.TreeSet[String]()
    keys ++= RequestedKeys
    keys
  }
}

/**
  * Trait that provides Tasks and other non-core classes with access to configuration.
  */
trait Configuration extends LazyLogging {
  private[config] val config = DagrConfig.config

  /**
    * Looks up a single value of a specific type in configuration. If the configuration key
    * does not exist, an exception is thrown. If the requested type is not supported, an
    * exception is thrown.
    */
  def configure[T : TypeTag](path: String) : T = {
    Configuration.RequestedKeys += path

    try {
      typeOf[T] match {
        case t if t =:= typeOf[String] => config.getString(path).asInstanceOf[T]
        case t if t =:= typeOf[Boolean] => config.getBoolean(path).asInstanceOf[T]
        case t if t =:= typeOf[Short] => config.getInt(path).toShort.asInstanceOf[T]
        case t if t =:= typeOf[Int] => config.getInt(path).asInstanceOf[T]
        case t if t =:= typeOf[Long] => config.getLong(path).asInstanceOf[T]
        case t if t =:= typeOf[Float] => config.getDouble(path).toFloat.asInstanceOf[T]
        case t if t =:= typeOf[Double] => config.getDouble(path).asInstanceOf[T]
        case t if t =:= typeOf[BigInt] => BigInt(config.getString(path)).asInstanceOf[T]
        case t if t =:= typeOf[BigDecimal] => BigDecimal(config.getString(path)).asInstanceOf[T]
        case t if t =:= typeOf[Path] => Paths.get(config.getString(path)).asInstanceOf[T]
        case t if t =:= typeOf[Cores] => Cores(config.getDouble(path).toFloat).asInstanceOf[T]
        case t if t =:= typeOf[Memory] => Memory(config.getString(path)).asInstanceOf[T]
        case t if t =:= typeOf[Duration] => config.getDuration(path).asInstanceOf[T]
        case _ => throw new IllegalArgumentException("Don't know how to configure a " + typeOf[T])
      }
    }
    catch {
      case ex : Exception =>
        logger.error(s"#############################################################################")
        logger.error(s"Exception retrieving configuration key '$path': ${ex.getMessage}")
        logger.error(s"#############################################################################")
        throw ex
    }
  }

  /**
    * Optionally accesses a configuration value. If the value is not present in the configuration
    * a None will be returned, else a Some(T) of the appropriate type.
    */
  def optionallyConfigure[T : TypeTag](path: String) : Option[T] = {
    Configuration.RequestedKeys += path
    if (config.hasPath(path)) Some(configure[T](path))
    else None
  }

  /**
    * Looks up a value in the configuration, and if present returns it, otherwise returns the default
    * value provided.
    */
  def configure[T : TypeTag](path: String, defaultValue: T) : T = {
    Configuration.RequestedKeys += path
    if (config.hasPath(path)) configure[T](path)
    else defaultValue
  }

  /**
    * Attempts to determine the path to an executable, first by looking it up in config,
    * and if that fails, by attempting to locate it on the system path. If both fail then
    * an exception is raised.
    *
    * @param path the configuration path to look up
    * @param executable the default name of the executable
    * @return An absolute path to the executable to use
    */
  def configureExecutable(path: String, executable: String) : Path = {
    Configuration.RequestedKeys += path

    optionallyConfigure[Path](path) match {
      case Some(exec) => exec
      case None => findInPath(executable) match {
        case Some(exec) => exec
        case None => throw new Generic(s"Could not configurable executable. Config path '$path' is not defined and executable '$executable' is not in PATH.")
      }
    }
  }

  /**
    * Attempts to determine the path to an executable.
    *
    * The config path is assumed to be the directory containing the executable.  If the config lookup fails, then we
    * attempt to locate it on the system path. If both fail then an exception is raised.  No validation is performed
    * that the executable actually exists at the returned path.
    *
    * @param binPath the configuration path to look up, representing the directory containing the executable
    * @param executable the default name of the executable
    * @return An absolute path to the executable to use
    */
  def configureExecutableFromBinDirectory(binPath: String, executable: String) : Path = {
    Configuration.RequestedKeys += binPath

    optionallyConfigure[Path](binPath) match {
      case Some(exec) =>
        Paths.get(exec.toString, executable)
      case None => findInPath(executable) match {
        case Some(exec) => exec
        case None => throw new Generic(s"Could not configurable executable. Config path '$binPath' is not defined and executable '$executable' is not in PATH.")
      }
    }
  }

  /** Searches the system path for the executable and return the full path. */
  private def findInPath(executable: String) : Option[Path] = {
    systemPath.map(p => p.resolve(executable)).find(ex => Files.exists(ex))
  }

  /**
    * Grabs the config key "PATH" which, if not defined in config will default to the environment variable
    * PATH, splits it on the path separator and returns it as a Seq[String]
    */
  private def systemPath : Seq[Path] = config.getString("dagr.path").split(File.pathSeparatorChar).view.map(Paths get _)
}