package gmt.core

import java.io.File

object Core {

    def create(args: List[String]): Core = {
        if (args.length < 1) {
            System.out.println("First argument must be the path to the setting files")
            System.exit(0)
        }

        new Core(Settings.read(new File(args(0))))
    }
}

case class Core private(settings: Settings)
