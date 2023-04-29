package moviesdb.testUtils

import moviesdb.domain.Movies.*

import scala.annotation.targetName

/*
case class ProductionYear(year: Int) extends AnyVal
case class MovieId(id: Int) extends AnyVal

// of course it should have a variant with ID, but I want to simplify the exercise
case class Episode(title: String, year: ProductionYear, number: Int)

sealed trait NewMovie
case class NewStandalone(title: String, year: ProductionYear) extends NewMovie
case class NewSeries(title: String, episodes: List[Episode]) extends NewMovie

sealed trait Movie extends Identifiable
case class Standalone(id: MovieId, title: String, year: ProductionYear) extends Movie
case class Series(id: MovieId, title: String, episodes: List[Episode]) extends Movie
*/



extension (newMovie: NewMovie)
  def withId(id: MovieId): Movie =
    newMovie match
      case NewStandalone(title, year) => Standalone(id, title, year)
      case NewSeries(title, episodes) => Series(id, title, episodes)
