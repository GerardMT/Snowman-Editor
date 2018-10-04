(define (domain snowman-adl)
    
    (:requirements
        :typing
        :negative-preconditions
        :equality
        :disjunctive-preconditions
        :conditional-effects
    )

    (:types 
        location direction ball size - object
    )

    (:predicates
        (snow ?l - location)
        (next ?from ?to - location ?dir - direction)
        (occupancy ?l - location)
        (character-at ?l - location)
        (ball-at ?b - ball ?l - location)
        (ball-size-small ?b - ball)
        (ball-size-medium ?b - ball)
        (ball-size-large ?b - ball)
        (goal)
    )
    
    (:action move_character
    
     :parameters
       (?from ?to - location ?dir - direction)

     :precondition
        (and
            (next ?from ?to ?dir)
            (character-at ?from)
            (not (occupancy ?to)))

     :effect
        (and
            (not (character-at ?from))
            (character-at ?to))
    )
    
    (:action move_ball

     :parameters
        (?b - ball ?ppos ?from ?to - location ?dir - direction)

     :precondition
        (and
            (next ?ppos ?from ?dir)
            (next ?from ?to ?dir)
            (ball-at ?b ?from)
            (character-at ?ppos)
            (forall (?o - ball)
                (or 
                    (= ?o ?b)
                    (or 
                        (not (ball-at ?o ?from))
                        (or 
                            (and
                                (ball-size-small ?b)
                                (ball-size-medium ?o))
                            (and
                                (ball-size-small ?b)
                                (ball-size-large ?o))
                            (and
                                (ball-size-medium ?b)
                                (ball-size-large ?o))))))
            (or
                (forall (?o - ball)
                    (or 
                        (= ?o ?b)
                        (not (ball-at ?o ?from))))
                (forall (?o - ball)
                        (not (ball-at ?o ?to))))
            (forall (?o - ball)
                    (or
                        (not (ball-at ?o ?to))
                        (or 
                            (and
                                (ball-size-small ?b)
                                (ball-size-medium ?o))
                            (and
                                (ball-size-small ?b)
                                (ball-size-large ?o))
                            (and
                                (ball-size-medium ?b)
                                (ball-size-large ?o))))))
      :effect
        (and
            (when
                (forall (?o - ball)
                    (or (= ?o ?b)
                    (ball-at ?o ?to)))
                (goal))
            (not (occupancy ?from))
            (occupancy ?to)
            (not (ball-at ?b ?from))
            (ball-at ?b ?to)
            (when
                (forall (?o - ball)
                    (or 
                        (= ?o ?b)
                        (not (ball-at ?o ?from))))
                (and
                    (not (character-at ?ppos))
                    (character-at ?from)))
            (not (snow ?to))
            (when
                (and
                    (snow ?to)
                    (ball-size-small ?b))
                (and
                    (not (ball-size-small ?b))
                    (ball-size-medium ?b)))
            (when
                (and
                    (snow ?to)
                    (ball-size-medium ?b))
                (and
                    (not (ball-size-medium ?b))
                    (ball-size-large ?b))))
    )
)
