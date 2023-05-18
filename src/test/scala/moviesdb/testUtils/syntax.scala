package moviesdb.testUtils

import moviesdb.domain.Movies.*

import scala.annotation.targetName

extension (newMovie: NewMovie)
  def withId(id: MovieId): Movie =
    newMovie match
      case NewStandalone(title, year) => Standalone(id, title, year)
      case NewSeries(title, episodes) => Series(id, title, episodes)
