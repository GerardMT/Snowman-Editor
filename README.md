# Snowman Editor

The Snowman Editor is an enhanced level editor for the the video game A Good Snowman Is Hard To Build. This project is 
an extension of my Computer Science final degree project (Tag v1.0: [documents](https://github.com/GerardMT/Snowman-Editor/docs/v1.0) and [source code](https://github.com/GerardMT/Snowman-Editor/tree/f0d80f820344c1d6b621d0c329613f4f683d2037)).

## Main features
- Modify and create new levels.
- Extract the the official game levels.
- Upload new levels to the game and try them.
- Solve a given level ***optimally*** using planning as satisfiability (SMT).
- Generate PDDL files to solve with external planners.

## Know issues / missing features
- In levels with multiple snowmen, once a snowman is build it can be destroyed (the game does not allow it).
- The tool currently does not support the "dream world".

## Installation
Download the latest version from the [release page](https://github.com/GerardMT/Snowman-Editor/releases) or [build](https://github.com/GerardMT/Snowman-Editor#build) the project.

The editor requires at least Java SE 11. 
    
## Usage
First execution to generate the config file:
```
java -jar snowman_editor.jar init
```

Once the config file has been generated, modify it. The editor can run without the game and/or the Yices 2 solver installed.

Example of configuration file (game downloaded from Steam):

    game_path=/home/snowman/Games/SteamLibrary/steamapps/common/A Good Snowman Is Hard To Build/
    save_path=/home/snowman/.local/share/a-good-snowman/
    solver_path=/opt/yices-smt2

Finally:
1. Run the editor in GUI mode:

       java -jar snowman_editor.jar snowman_editor.config gui
       
    The SMT Solver results will be displayed in the terminal.

2. Or use the terminal mode. Example of solving a level (minimizing ball movements):
       
       java -jar snowman_editor.jar snowman_editor.config smt-reachability ./levels/toy/toy05.lvl ./out auto 1000 true true true

    To see all available options:

       java -jar snowman_editor.jar -h


A more detailed (but not up to date) manual can be found in [docs/v1.0/report](https://github.com/GerardMT/Snowman-Editor/tree/master/docs/v1.0/report).

## Build
Download the source code:

    git clone https://github.com/GerardMT/Snowman-Editor
    cd Snowman-Editor

The project uses the SBT build tool. To compile the project run: 
    
    sbt compile
    sbt assembly
    
The package output can be found at ``./out/snowman_editor.jar``.

I personally use IntelliJ IDEA which eases the Scala/Java environment setup.

## Screenshots
![Editor Andy](docs/screenshots/snowman_editor_andy.png?raw=true) ![Game Levels](docs/screenshots/snowman_editor_game_levels.png?raw=true) ![Game Andy](docs/screenshots/game_andy.png?raw=true) ![Solver options](docs/screenshots/snowman_editor_solver.png?raw=true) ![Andy SMT Reachability 1](docs/screenshots/snowman_editor_terminal_smt_reachability_andy_1.png?raw=true) ![Andy SMT Reachability 2](docs/screenshots/snowman_editor_terminal_smt_reachability_andy_2.png?raw=true)

## Other
A simpler version for the Sokoban game can be found in [Sokoban-Editor](https://github.com/GerardMT/Sokoban-Editor).    