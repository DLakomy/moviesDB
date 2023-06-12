package moviesdb.testUtils.syntax

import moviesdb.domain.Movies.*
import cats.instances.ordering.*
import cats.implicits.*

object MoviesSyntax:
  extension (standalone: Standalone)
    def normalised: Standalone = standalone // it's ok already

  extension (series: Series)
    // episodes should be ordered in a deterministic way
    def normalised: Series = series.copy(episodes = series.episodes.sortBy(_.hashCode))

  extension (movie: Movie)
    def normalised: Movie =
      movie match
        case s: Standalone => s.normalised
        case s: Series => s.normalised

  // sorts by id and normalises every element
  extension (movieList: List[Movie])
    def normalised: List[Movie] =
      movieList.map(_.normalised).sortBy(_.id.value)
