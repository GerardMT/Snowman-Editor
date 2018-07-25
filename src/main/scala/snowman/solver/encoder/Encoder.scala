package snowman.solver.encoder

import snowman.level.Level

abstract class Encoder {

    def encode(l: Level, actions: Int): Encoding
}
