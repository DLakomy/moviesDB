package moviesdb.core.syntax

import moviesdb.domain.Movies.*

object MoviesSyntax:
  extension (newStandalone: NewStandalone)
    def withId(id: MovieId): Standalone = Standalone(id, newStandalone.title, newStandalone.year)

  extension (newSeries: NewSeries)
    def withId(id: MovieId): Series = Series(id, newSeries.title, newSeries.episodes)

  extension (newMovie: NewMovie)
    def withId(id: MovieId): Movie =
      newMovie match
        case s: NewStandalone => s.withId(id)
        case s: NewSeries => s.withId(id)
