package cl.mixin.stdnum.mx

import cl.mixin.stdnum.InvalidChecksum
import cl.mixin.stdnum.InvalidComponent
import cl.mixin.stdnum.InvalidFormat
import cl.mixin.stdnum.InvalidLength
import cl.mixin.stdnum.Tools
import cl.mixin.stdnum.ValidationError
import java.util.Date
import scala.util.Random
import scala.util.Try
import scala.util.matching.Regex

/** RFC (Registro Federal de Contribuyentes, Mexican tax number). */
object Rfc {
  // these values should not appear as first part of a personal number
  private val NAME_BLACKLIST = Vector(
    "BUEI",
    "BUEY",
    "CACA",
    "CACO",
    "CAGA",
    "CAGO",
    "CAKA",
    "CAKO",
    "COGE",
    "COJA",
    "COJE",
    "COJI",
    "COJO",
    "CULO",
    "FETO",
    "GUEY",
    "JOTO",
    "KACA",
    "KACO",
    "KAGA",
    "KAGO",
    "KAKA",
    "KOGE",
    "KOJO",
    "KULO",
    "MAME",
    "MAMO",
    "MEAR",
    "MEAS",
    "MEON",
    "MION",
    "MOCO",
    "MULA",
    "PEDA",
    "PEDO",
    "PENE",
    "PUTA",
    "PUTO",
    "QULO",
    "RATA",
    "RUIN"
  )

  // characters used for checksum calculation,
  private val ALPHABET = "0123456789ABCDEFGHIJKLMN&OPQRSTUVWXYZ Ñ"

  /** Convert the part of the number that represents a date into a datetime. Note that the century
    * may be incorrect.
    */
  private def getDate(number: String): Try[Date] =
    Try {
      val year = number.take(2).toInt
      val month = number.drop(2).take(2).toInt
      val day = number.drop(4).take(2).toInt
      Date(year + 2000, month, day)
    }

  /** Check if the number is a valid RFC. */
  def isValid(number: String): Boolean = this.validate(number).isRight

  /** Convert the number to the minimal representation. This strips the number of any valid
    * separators and removes surrounding whitespace.
    */
  def compact(number: String): String =
    return Tools.clean(number, Vector('-', '_', ' ')).toUpperCase.strip

  /** Check if the number is a valid RUT. This checks the length, formatting and check digit.
    */
  def validate(
    number: String,
    validateCheckDigit: Boolean = false
  ): Either[ValidationError, String] =

    def validateCs(validateCheckDigit: Boolean, number: String) =
      if validateCheckDigit && number.length >= 12 then
        if !"^[1-9A-V][1-9A-Z][0-9A]$".r.matches(number.takeRight(3)) then Left(InvalidComponent())
        else if number.last != this.calcCheckDigit(number.init) then Left(InvalidChecksum())
        else Right(number)
      else Right(number)

    val compatNumber = this.compact(number)
    val compactNumberLength = compatNumber.length
    if compactNumberLength == 10 || compactNumberLength == 13 then
      if !"^[A-Z&Ñ]{4}[0-9]{6}[0-9A-Z]{0,3}$".r.matches(compatNumber) then Left(InvalidFormat())
      else if NAME_BLACKLIST.contains(compatNumber.take(4)) then Left(InvalidComponent())
      else if this.getDate(compatNumber.drop(4).take(6)).isFailure then Left(InvalidComponent())
      else validateCs(validateCheckDigit, compatNumber)
    else if compactNumberLength == 12 then
      if !"^[A-Z&Ñ]{3}[0-9]{6}[0-9A-Z]{3}$".r.matches(compatNumber) then Left(InvalidFormat())
      else if this.getDate(compatNumber.drop(3).take(6)).isFailure then Left(InvalidComponent())
      else validateCs(validateCheckDigit, compatNumber)
    else Left(InvalidLength())

  /** Reformat the number to the standard presentation format. */
  def format(number: String, separator: String = " "): String =
    val fNumber = this.compact(number)
    if number.length == 12 then
      s"${fNumber.take(3)}$separator${fNumber.drop(3).take(6)}$separator${fNumber.drop(9)}"
    else s"${fNumber.take(4)}$separator${fNumber.drop(4).take(6)}$separator${fNumber.drop(10)}"

  /** Calculate the check digit. The number passed should not have the check digit included.
    */
  private def calcCheckDigit(number: String): Char =
    val concatNumber = s"   $number".takeRight(12)
    val check = number.zipWithIndex.map((n, i) => ALPHABET.indexOf(n.toChar) * (13 - i)).sum
    val index = (11 - check) % 11
    if index < 0 then ALPHABET(ALPHABET.length - math.abs(index)) else ALPHABET(index)
}
