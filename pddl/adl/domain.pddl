(define (domain snowman_adl)
    
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
        (character_at ?l - location)
        (ball_at ?b - ball ?l - location)
        (ball_size_small ?b - ball)
        (ball_size_medium ?b - ball)
        (ball_size_large ?b - ball)
        (goal)
    )
    
    (:action move_character
    
     :parameters
       (?from ?to - location ?dir - direction)

     :precondition
        (and
            (next ?from ?to ?dir)
            (character_at ?from)
            (not (occupancy ?to)))

     :effect
        (and
            (not (character_at ?from))
            (character_at ?to))
    )
    
    (:action move_ball

     :parameters
        (?b - ball ?ppos ?from ?to - location ?dir - direction)

     :precondition
        (and
            (next ?ppos ?from ?dir)
            (next ?from ?to ?dir)
            (ball_at ?b ?from)
            (character_at ?ppos)
            (forall (?o - ball)
                (or 
                    (= ?o ?b)
                    (or 
                        (not (ball_at ?o ?from))
                        (or 
                            (and
                                (ball_size_small ?b)
                                (ball_size_medium ?o))
                            (and
                                (ball_size_small ?b)
                                (ball_size_large ?o))
                            (and
                                (ball_size_medium ?b)
                                (ball_size_large ?o))))))
            (or
                (forall (?o - ball)
                    (or 
                        (= ?o ?b)
                        (not (ball_at ?o ?from))))
                (forall (?o - ball)
                        (not (ball_at ?o ?to))))
            (forall (?o - ball)
                    (or
                        (not (ball_at ?o ?to))
                        (or 
                            (and
                                (ball_size_small ?b)
                                (ball_size_medium ?o))
                            (and
                                (ball_size_small ?b)
                                (ball_size_large ?o))
                            (and
                                (ball_size_medium ?b)
                                (ball_size_large ?o))))))
      :effect
        (and
            (when
                (forall (?o - ball)
                    (or (= ?o ?b)
                    (ball_at ?o ?to)))
                (goal))
            (not (occupancy ?from))
            (occupancy ?to)
            (not (ball_at ?b ?from))
            (ball_at ?b ?to)
            (when
                (forall (?o - ball)
                    (or 
                        (= ?o ?b)
                        (not (ball_at ?o ?from))))
                (and
                    (not (character_at ?ppos))
                    (character_at ?from)))
            (not (snow ?to))
            (when
                (and
                    (snow ?to)
                    (ball_size_small ?b))
                (and
                    (not (ball_size_small ?b))
                    (ball_size_medium ?b)))
            (when
                (and
                    (snow ?to)
                    (ball_size_medium ?b))
                (and
                    (not (ball_size_medium ?b))
                    (ball_size_large ?b))))
    )
)
