(define (domain snowman_adl_grounded)

    (:requirements
        :typing
        :negative-preconditions
        :equality
        :disjunctive-preconditions
        :conditional-effects
    )

    (:types
        location ball - object
    )

    (:constants
        ball_0 - ball
        ball_1 - ball
        ball_2 - ball
        loc_1_1 - location
        loc_1_2 - location
        loc_1_3 - location
        loc_1_4 - location
    )

    (:predicates
        (snow ?l - location)
        (occupancy ?l - location)
        (character_at ?l - location)
        (ball_at ?b - ball ?l - location)
        (ball_size_small ?b - ball)
        (ball_size_medium ?b - ball)
        (ball_size_large ?b - ball)
        (goal)
    )

    (:action move_character__loc_1_1__loc_1_2

     :parameters
        ()

     :precondition
        (and
            (character_at loc_1_1)
            (not (occupancy loc_1_2)))

     :effect
       (and
           (not (character_at loc_1_1))
           (character_at loc_1_2))
    )

    (:action move_character__loc_1_2__loc_1_3

     :parameters
        ()

     :precondition
        (and
            (character_at loc_1_2)
            (not (occupancy loc_1_3)))

     :effect
       (and
           (not (character_at loc_1_2))
           (character_at loc_1_3))
    )

    (:action move_character__loc_1_2__loc_1_1

     :parameters
        ()

     :precondition
        (and
            (character_at loc_1_2)
            (not (occupancy loc_1_1)))

     :effect
       (and
           (not (character_at loc_1_2))
           (character_at loc_1_1))
    )

    (:action move_character__loc_1_3__loc_1_4

     :parameters
        ()

     :precondition
        (and
            (character_at loc_1_3)
            (not (occupancy loc_1_4)))

     :effect
       (and
           (not (character_at loc_1_3))
           (character_at loc_1_4))
    )

    (:action move_character__loc_1_3__loc_1_2

     :parameters
        ()

     :precondition
        (and
            (character_at loc_1_3)
            (not (occupancy loc_1_2)))

     :effect
       (and
           (not (character_at loc_1_3))
           (character_at loc_1_2))
    )

    (:action move_character__loc_1_4__loc_1_3

     :parameters
        ()

     :precondition
        (and
            (character_at loc_1_4)
            (not (occupancy loc_1_3)))

     :effect
       (and
           (not (character_at loc_1_4))
           (character_at loc_1_3))
    )

    (:action move_ball__ball_0__loc_1_1__loc_1_2__loc_1_3__ball_1__ball_2

     :parameters
        ()

     :precondition
        (and
            (ball_at ball_0 loc_1_2)
            (character_at loc_1_1)
            (and
                (or
                    (not (ball_at ball_1 loc_1_2))
                    (or
                        (and
                            (ball_size_small ball_0)
                            (ball_size_medium ball_1))
                        (and
                            (ball_size_small ball_0)
                            (ball_size_large ball_1))
                        (and
                            (ball_size_medium ball_0)
                            (ball_size_large ball_1))))
                (or
                    (not (ball_at ball_2 loc_1_2))
                    (or
                        (and
                            (ball_size_small ball_0)
                            (ball_size_medium ball_2))
                        (and
                            (ball_size_small ball_0)
                            (ball_size_large ball_2))
                        (and
                            (ball_size_medium ball_0)
                            (ball_size_large ball_2)))))
            (or
                (not (or
                    (ball_at ball_1 loc_1_2)
                    (ball_at ball_2 loc_1_2)))
                (and

                    (not (ball_at ball_1 loc_1_3))
                    (not (ball_at ball_2 loc_1_3))))
            (and
                (or
                    (not (ball_at ball_1 loc_1_3))
                    (or
                        (and
                            (ball_size_small ball_0)
                            (ball_size_medium ball_1))
                        (and
                            (ball_size_small ball_0)
                            (ball_size_large ball_1))
                        (and
                            (ball_size_medium ball_0)
                            (ball_size_large ball_1))))
                (or
                    (not (ball_at ball_2 loc_1_3))
                    (or
                        (and
                            (ball_size_small ball_0)
                            (ball_size_medium ball_2))
                        (and
                            (ball_size_small ball_0)
                            (ball_size_large ball_2))
                        (and
                            (ball_size_medium ball_0)
                            (ball_size_large ball_2))))))

     :effect
        (and
            (when
                (and
                    (ball_at ball_1 loc_1_3)
                    (ball_at ball_2 loc_1_3))
                (goal))
            (not (occupancy loc_1_2))
            (occupancy loc_1_3)
            (not (ball_at ball_0 loc_1_2))
            (ball_at ball_0 loc_1_3)
            (when
                (and
                    (not (ball_at ball_1 loc_1_2))
                    (not (ball_at ball_2 loc_1_2)))
                (and
                    (not (character_at loc_1_1))
                    (character_at loc_1_2)))
            (not (snow loc_1_3))
            (when
                (and
                    (snow loc_1_3)
                    (ball_size_small ball_0))
                (and
                    (not (ball_size_small ball_0))
                    (ball_size_medium ball_0)))
            (when
                (and
                    (snow loc_1_3)
                    (ball_size_medium ball_0))
                (and                    (not (ball_size_medium ball_0))
                    (ball_size_large ball_0))))
    )

    (:action move_ball__ball_1__loc_1_1__loc_1_2__loc_1_3__ball_0__ball_2

     :parameters
        ()

     :precondition
        (and
            (ball_at ball_1 loc_1_2)
            (character_at loc_1_1)
            (and
                (or
                    (not (ball_at ball_0 loc_1_2))
                    (or
                        (and
                            (ball_size_small ball_1)
                            (ball_size_medium ball_0))
                        (and
                            (ball_size_small ball_1)
                            (ball_size_large ball_0))
                        (and
                            (ball_size_medium ball_1)
                            (ball_size_large ball_0))))
                (or
                    (not (ball_at ball_2 loc_1_2))
                    (or
                        (and
                            (ball_size_small ball_1)
                            (ball_size_medium ball_2))
                        (and
                            (ball_size_small ball_1)
                            (ball_size_large ball_2))
                        (and
                            (ball_size_medium ball_1)
                            (ball_size_large ball_2)))))
            (or
                (not (or
                    (ball_at ball_0 loc_1_2)
                    (ball_at ball_2 loc_1_2)))
                (and

                    (not (ball_at ball_0 loc_1_3))
                    (not (ball_at ball_2 loc_1_3))))
            (and
                (or
                    (not (ball_at ball_0 loc_1_3))
                    (or
                        (and
                            (ball_size_small ball_1)
                            (ball_size_medium ball_0))
                        (and
                            (ball_size_small ball_1)
                            (ball_size_large ball_0))
                        (and
                            (ball_size_medium ball_1)
                            (ball_size_large ball_0))))
                (or
                    (not (ball_at ball_2 loc_1_3))
                    (or
                        (and
                            (ball_size_small ball_1)
                            (ball_size_medium ball_2))
                        (and
                            (ball_size_small ball_1)
                            (ball_size_large ball_2))
                        (and
                            (ball_size_medium ball_1)
                            (ball_size_large ball_2))))))

     :effect
        (and
            (when
                (and
                    (ball_at ball_0 loc_1_3)
                    (ball_at ball_2 loc_1_3))
                (goal))
            (not (occupancy loc_1_2))
            (occupancy loc_1_3)
            (not (ball_at ball_1 loc_1_2))
            (ball_at ball_1 loc_1_3)
            (when
                (and
                    (not (ball_at ball_0 loc_1_2))
                    (not (ball_at ball_2 loc_1_2)))
                (and
                    (not (character_at loc_1_1))
                    (character_at loc_1_2)))
            (not (snow loc_1_3))
            (when
                (and
                    (snow loc_1_3)
                    (ball_size_small ball_1))
                (and
                    (not (ball_size_small ball_1))
                    (ball_size_medium ball_1)))
            (when
                (and
                    (snow loc_1_3)
                    (ball_size_medium ball_1))
                (and                    (not (ball_size_medium ball_1))
                    (ball_size_large ball_1))))
    )

    (:action move_ball__ball_2__loc_1_1__loc_1_2__loc_1_3__ball_0__ball_1

     :parameters
        ()

     :precondition
        (and
            (ball_at ball_2 loc_1_2)
            (character_at loc_1_1)
            (and
                (or
                    (not (ball_at ball_0 loc_1_2))
                    (or
                        (and
                            (ball_size_small ball_2)
                            (ball_size_medium ball_0))
                        (and
                            (ball_size_small ball_2)
                            (ball_size_large ball_0))
                        (and
                            (ball_size_medium ball_2)
                            (ball_size_large ball_0))))
                (or
                    (not (ball_at ball_1 loc_1_2))
                    (or
                        (and
                            (ball_size_small ball_2)
                            (ball_size_medium ball_1))
                        (and
                            (ball_size_small ball_2)
                            (ball_size_large ball_1))
                        (and
                            (ball_size_medium ball_2)
                            (ball_size_large ball_1)))))
            (or
                (not (or
                    (ball_at ball_0 loc_1_2)
                    (ball_at ball_1 loc_1_2)))
                (and

                    (not (ball_at ball_0 loc_1_3))
                    (not (ball_at ball_1 loc_1_3))))
            (and
                (or
                    (not (ball_at ball_0 loc_1_3))
                    (or
                        (and
                            (ball_size_small ball_2)
                            (ball_size_medium ball_0))
                        (and
                            (ball_size_small ball_2)
                            (ball_size_large ball_0))
                        (and
                            (ball_size_medium ball_2)
                            (ball_size_large ball_0))))
                (or
                    (not (ball_at ball_1 loc_1_3))
                    (or
                        (and
                            (ball_size_small ball_2)
                            (ball_size_medium ball_1))
                        (and
                            (ball_size_small ball_2)
                            (ball_size_large ball_1))
                        (and
                            (ball_size_medium ball_2)
                            (ball_size_large ball_1))))))

     :effect
        (and
            (when
                (and
                    (ball_at ball_0 loc_1_3)
                    (ball_at ball_1 loc_1_3))
                (goal))
            (not (occupancy loc_1_2))
            (occupancy loc_1_3)
            (not (ball_at ball_2 loc_1_2))
            (ball_at ball_2 loc_1_3)
            (when
                (and
                    (not (ball_at ball_0 loc_1_2))
                    (not (ball_at ball_1 loc_1_2)))
                (and
                    (not (character_at loc_1_1))
                    (character_at loc_1_2)))
            (not (snow loc_1_3))
            (when
                (and
                    (snow loc_1_3)
                    (ball_size_small ball_2))
                (and
                    (not (ball_size_small ball_2))
                    (ball_size_medium ball_2)))
            (when
                (and
                    (snow loc_1_3)
                    (ball_size_medium ball_2))
                (and                    (not (ball_size_medium ball_2))
                    (ball_size_large ball_2))))
    )

    (:action move_ball__ball_0__loc_1_2__loc_1_3__loc_1_4__ball_1__ball_2

     :parameters
        ()

     :precondition
        (and
            (ball_at ball_0 loc_1_3)
            (character_at loc_1_2)
            (and
                (or
                    (not (ball_at ball_1 loc_1_3))
                    (or
                        (and
                            (ball_size_small ball_0)
                            (ball_size_medium ball_1))
                        (and
                            (ball_size_small ball_0)
                            (ball_size_large ball_1))
                        (and
                            (ball_size_medium ball_0)
                            (ball_size_large ball_1))))
                (or
                    (not (ball_at ball_2 loc_1_3))
                    (or
                        (and
                            (ball_size_small ball_0)
                            (ball_size_medium ball_2))
                        (and
                            (ball_size_small ball_0)
                            (ball_size_large ball_2))
                        (and
                            (ball_size_medium ball_0)
                            (ball_size_large ball_2)))))
            (or
                (not (or
                    (ball_at ball_1 loc_1_3)
                    (ball_at ball_2 loc_1_3)))
                (and

                    (not (ball_at ball_1 loc_1_4))
                    (not (ball_at ball_2 loc_1_4))))
            (and
                (or
                    (not (ball_at ball_1 loc_1_4))
                    (or
                        (and
                            (ball_size_small ball_0)
                            (ball_size_medium ball_1))
                        (and
                            (ball_size_small ball_0)
                            (ball_size_large ball_1))
                        (and
                            (ball_size_medium ball_0)
                            (ball_size_large ball_1))))
                (or
                    (not (ball_at ball_2 loc_1_4))
                    (or
                        (and
                            (ball_size_small ball_0)
                            (ball_size_medium ball_2))
                        (and
                            (ball_size_small ball_0)
                            (ball_size_large ball_2))
                        (and
                            (ball_size_medium ball_0)
                            (ball_size_large ball_2))))))

     :effect
        (and
            (when
                (and
                    (ball_at ball_1 loc_1_4)
                    (ball_at ball_2 loc_1_4))
                (goal))
            (not (occupancy loc_1_3))
            (occupancy loc_1_4)
            (not (ball_at ball_0 loc_1_3))
            (ball_at ball_0 loc_1_4)
            (when
                (and
                    (not (ball_at ball_1 loc_1_3))
                    (not (ball_at ball_2 loc_1_3)))
                (and
                    (not (character_at loc_1_2))
                    (character_at loc_1_3)))
            (not (snow loc_1_4))
            (when
                (and
                    (snow loc_1_4)
                    (ball_size_small ball_0))
                (and
                    (not (ball_size_small ball_0))
                    (ball_size_medium ball_0)))
            (when
                (and
                    (snow loc_1_4)
                    (ball_size_medium ball_0))
                (and                    (not (ball_size_medium ball_0))
                    (ball_size_large ball_0))))
    )

    (:action move_ball__ball_1__loc_1_2__loc_1_3__loc_1_4__ball_0__ball_2

     :parameters
        ()

     :precondition
        (and
            (ball_at ball_1 loc_1_3)
            (character_at loc_1_2)
            (and
                (or
                    (not (ball_at ball_0 loc_1_3))
                    (or
                        (and
                            (ball_size_small ball_1)
                            (ball_size_medium ball_0))
                        (and
                            (ball_size_small ball_1)
                            (ball_size_large ball_0))
                        (and
                            (ball_size_medium ball_1)
                            (ball_size_large ball_0))))
                (or
                    (not (ball_at ball_2 loc_1_3))
                    (or
                        (and
                            (ball_size_small ball_1)
                            (ball_size_medium ball_2))
                        (and
                            (ball_size_small ball_1)
                            (ball_size_large ball_2))
                        (and
                            (ball_size_medium ball_1)
                            (ball_size_large ball_2)))))
            (or
                (not (or
                    (ball_at ball_0 loc_1_3)
                    (ball_at ball_2 loc_1_3)))
                (and

                    (not (ball_at ball_0 loc_1_4))
                    (not (ball_at ball_2 loc_1_4))))
            (and
                (or
                    (not (ball_at ball_0 loc_1_4))
                    (or
                        (and
                            (ball_size_small ball_1)
                            (ball_size_medium ball_0))
                        (and
                            (ball_size_small ball_1)
                            (ball_size_large ball_0))
                        (and
                            (ball_size_medium ball_1)
                            (ball_size_large ball_0))))
                (or
                    (not (ball_at ball_2 loc_1_4))
                    (or
                        (and
                            (ball_size_small ball_1)
                            (ball_size_medium ball_2))
                        (and
                            (ball_size_small ball_1)
                            (ball_size_large ball_2))
                        (and
                            (ball_size_medium ball_1)
                            (ball_size_large ball_2))))))

     :effect
        (and
            (when
                (and
                    (ball_at ball_0 loc_1_4)
                    (ball_at ball_2 loc_1_4))
                (goal))
            (not (occupancy loc_1_3))
            (occupancy loc_1_4)
            (not (ball_at ball_1 loc_1_3))
            (ball_at ball_1 loc_1_4)
            (when
                (and
                    (not (ball_at ball_0 loc_1_3))
                    (not (ball_at ball_2 loc_1_3)))
                (and
                    (not (character_at loc_1_2))
                    (character_at loc_1_3)))
            (not (snow loc_1_4))
            (when
                (and
                    (snow loc_1_4)
                    (ball_size_small ball_1))
                (and
                    (not (ball_size_small ball_1))
                    (ball_size_medium ball_1)))
            (when
                (and
                    (snow loc_1_4)
                    (ball_size_medium ball_1))
                (and                    (not (ball_size_medium ball_1))
                    (ball_size_large ball_1))))
    )

    (:action move_ball__ball_2__loc_1_2__loc_1_3__loc_1_4__ball_0__ball_1

     :parameters
        ()

     :precondition
        (and
            (ball_at ball_2 loc_1_3)
            (character_at loc_1_2)
            (and
                (or
                    (not (ball_at ball_0 loc_1_3))
                    (or
                        (and
                            (ball_size_small ball_2)
                            (ball_size_medium ball_0))
                        (and
                            (ball_size_small ball_2)
                            (ball_size_large ball_0))
                        (and
                            (ball_size_medium ball_2)
                            (ball_size_large ball_0))))
                (or
                    (not (ball_at ball_1 loc_1_3))
                    (or
                        (and
                            (ball_size_small ball_2)
                            (ball_size_medium ball_1))
                        (and
                            (ball_size_small ball_2)
                            (ball_size_large ball_1))
                        (and
                            (ball_size_medium ball_2)
                            (ball_size_large ball_1)))))
            (or
                (not (or
                    (ball_at ball_0 loc_1_3)
                    (ball_at ball_1 loc_1_3)))
                (and

                    (not (ball_at ball_0 loc_1_4))
                    (not (ball_at ball_1 loc_1_4))))
            (and
                (or
                    (not (ball_at ball_0 loc_1_4))
                    (or
                        (and
                            (ball_size_small ball_2)
                            (ball_size_medium ball_0))
                        (and
                            (ball_size_small ball_2)
                            (ball_size_large ball_0))
                        (and
                            (ball_size_medium ball_2)
                            (ball_size_large ball_0))))
                (or
                    (not (ball_at ball_1 loc_1_4))
                    (or
                        (and
                            (ball_size_small ball_2)
                            (ball_size_medium ball_1))
                        (and
                            (ball_size_small ball_2)
                            (ball_size_large ball_1))
                        (and
                            (ball_size_medium ball_2)
                            (ball_size_large ball_1))))))

     :effect
        (and
            (when
                (and
                    (ball_at ball_0 loc_1_4)
                    (ball_at ball_1 loc_1_4))
                (goal))
            (not (occupancy loc_1_3))
            (occupancy loc_1_4)
            (not (ball_at ball_2 loc_1_3))
            (ball_at ball_2 loc_1_4)
            (when
                (and
                    (not (ball_at ball_0 loc_1_3))
                    (not (ball_at ball_1 loc_1_3)))
                (and
                    (not (character_at loc_1_2))
                    (character_at loc_1_3)))
            (not (snow loc_1_4))
            (when
                (and
                    (snow loc_1_4)
                    (ball_size_small ball_2))
                (and
                    (not (ball_size_small ball_2))
                    (ball_size_medium ball_2)))
            (when
                (and
                    (snow loc_1_4)
                    (ball_size_medium ball_2))
                (and                    (not (ball_size_medium ball_2))
                    (ball_size_large ball_2))))
    )

    (:action move_ball__ball_0__loc_1_3__loc_1_2__loc_1_1__ball_1__ball_2

     :parameters
        ()

     :precondition
        (and
            (ball_at ball_0 loc_1_2)
            (character_at loc_1_3)
            (and
                (or
                    (not (ball_at ball_1 loc_1_2))
                    (or
                        (and
                            (ball_size_small ball_0)
                            (ball_size_medium ball_1))
                        (and
                            (ball_size_small ball_0)
                            (ball_size_large ball_1))
                        (and
                            (ball_size_medium ball_0)
                            (ball_size_large ball_1))))
                (or
                    (not (ball_at ball_2 loc_1_2))
                    (or
                        (and
                            (ball_size_small ball_0)
                            (ball_size_medium ball_2))
                        (and
                            (ball_size_small ball_0)
                            (ball_size_large ball_2))
                        (and
                            (ball_size_medium ball_0)
                            (ball_size_large ball_2)))))
            (or
                (not (or
                    (ball_at ball_1 loc_1_2)
                    (ball_at ball_2 loc_1_2)))
                (and

                    (not (ball_at ball_1 loc_1_1))
                    (not (ball_at ball_2 loc_1_1))))
            (and
                (or
                    (not (ball_at ball_1 loc_1_1))
                    (or
                        (and
                            (ball_size_small ball_0)
                            (ball_size_medium ball_1))
                        (and
                            (ball_size_small ball_0)
                            (ball_size_large ball_1))
                        (and
                            (ball_size_medium ball_0)
                            (ball_size_large ball_1))))
                (or
                    (not (ball_at ball_2 loc_1_1))
                    (or
                        (and
                            (ball_size_small ball_0)
                            (ball_size_medium ball_2))
                        (and
                            (ball_size_small ball_0)
                            (ball_size_large ball_2))
                        (and
                            (ball_size_medium ball_0)
                            (ball_size_large ball_2))))))

     :effect
        (and
            (when
                (and
                    (ball_at ball_1 loc_1_1)
                    (ball_at ball_2 loc_1_1))
                (goal))
            (not (occupancy loc_1_2))
            (occupancy loc_1_1)
            (not (ball_at ball_0 loc_1_2))
            (ball_at ball_0 loc_1_1)
            (when
                (and
                    (not (ball_at ball_1 loc_1_2))
                    (not (ball_at ball_2 loc_1_2)))
                (and
                    (not (character_at loc_1_3))
                    (character_at loc_1_2)))
            (not (snow loc_1_1))
            (when
                (and
                    (snow loc_1_1)
                    (ball_size_small ball_0))
                (and
                    (not (ball_size_small ball_0))
                    (ball_size_medium ball_0)))
            (when
                (and
                    (snow loc_1_1)
                    (ball_size_medium ball_0))
                (and                    (not (ball_size_medium ball_0))
                    (ball_size_large ball_0))))
    )

    (:action move_ball__ball_1__loc_1_3__loc_1_2__loc_1_1__ball_0__ball_2

     :parameters
        ()

     :precondition
        (and
            (ball_at ball_1 loc_1_2)
            (character_at loc_1_3)
            (and
                (or
                    (not (ball_at ball_0 loc_1_2))
                    (or
                        (and
                            (ball_size_small ball_1)
                            (ball_size_medium ball_0))
                        (and
                            (ball_size_small ball_1)
                            (ball_size_large ball_0))
                        (and
                            (ball_size_medium ball_1)
                            (ball_size_large ball_0))))
                (or
                    (not (ball_at ball_2 loc_1_2))
                    (or
                        (and
                            (ball_size_small ball_1)
                            (ball_size_medium ball_2))
                        (and
                            (ball_size_small ball_1)
                            (ball_size_large ball_2))
                        (and
                            (ball_size_medium ball_1)
                            (ball_size_large ball_2)))))
            (or
                (not (or
                    (ball_at ball_0 loc_1_2)
                    (ball_at ball_2 loc_1_2)))
                (and

                    (not (ball_at ball_0 loc_1_1))
                    (not (ball_at ball_2 loc_1_1))))
            (and
                (or
                    (not (ball_at ball_0 loc_1_1))
                    (or
                        (and
                            (ball_size_small ball_1)
                            (ball_size_medium ball_0))
                        (and
                            (ball_size_small ball_1)
                            (ball_size_large ball_0))
                        (and
                            (ball_size_medium ball_1)
                            (ball_size_large ball_0))))
                (or
                    (not (ball_at ball_2 loc_1_1))
                    (or
                        (and
                            (ball_size_small ball_1)
                            (ball_size_medium ball_2))
                        (and
                            (ball_size_small ball_1)
                            (ball_size_large ball_2))
                        (and
                            (ball_size_medium ball_1)
                            (ball_size_large ball_2))))))

     :effect
        (and
            (when
                (and
                    (ball_at ball_0 loc_1_1)
                    (ball_at ball_2 loc_1_1))
                (goal))
            (not (occupancy loc_1_2))
            (occupancy loc_1_1)
            (not (ball_at ball_1 loc_1_2))
            (ball_at ball_1 loc_1_1)
            (when
                (and
                    (not (ball_at ball_0 loc_1_2))
                    (not (ball_at ball_2 loc_1_2)))
                (and
                    (not (character_at loc_1_3))
                    (character_at loc_1_2)))
            (not (snow loc_1_1))
            (when
                (and
                    (snow loc_1_1)
                    (ball_size_small ball_1))
                (and
                    (not (ball_size_small ball_1))
                    (ball_size_medium ball_1)))
            (when
                (and
                    (snow loc_1_1)
                    (ball_size_medium ball_1))
                (and                    (not (ball_size_medium ball_1))
                    (ball_size_large ball_1))))
    )

    (:action move_ball__ball_2__loc_1_3__loc_1_2__loc_1_1__ball_0__ball_1

     :parameters
        ()

     :precondition
        (and
            (ball_at ball_2 loc_1_2)
            (character_at loc_1_3)
            (and
                (or
                    (not (ball_at ball_0 loc_1_2))
                    (or
                        (and
                            (ball_size_small ball_2)
                            (ball_size_medium ball_0))
                        (and
                            (ball_size_small ball_2)
                            (ball_size_large ball_0))
                        (and
                            (ball_size_medium ball_2)
                            (ball_size_large ball_0))))
                (or
                    (not (ball_at ball_1 loc_1_2))
                    (or
                        (and
                            (ball_size_small ball_2)
                            (ball_size_medium ball_1))
                        (and
                            (ball_size_small ball_2)
                            (ball_size_large ball_1))
                        (and
                            (ball_size_medium ball_2)
                            (ball_size_large ball_1)))))
            (or
                (not (or
                    (ball_at ball_0 loc_1_2)
                    (ball_at ball_1 loc_1_2)))
                (and

                    (not (ball_at ball_0 loc_1_1))
                    (not (ball_at ball_1 loc_1_1))))
            (and
                (or
                    (not (ball_at ball_0 loc_1_1))
                    (or
                        (and
                            (ball_size_small ball_2)
                            (ball_size_medium ball_0))
                        (and
                            (ball_size_small ball_2)
                            (ball_size_large ball_0))
                        (and
                            (ball_size_medium ball_2)
                            (ball_size_large ball_0))))
                (or
                    (not (ball_at ball_1 loc_1_1))
                    (or
                        (and
                            (ball_size_small ball_2)
                            (ball_size_medium ball_1))
                        (and
                            (ball_size_small ball_2)
                            (ball_size_large ball_1))
                        (and
                            (ball_size_medium ball_2)
                            (ball_size_large ball_1))))))

     :effect
        (and
            (when
                (and
                    (ball_at ball_0 loc_1_1)
                    (ball_at ball_1 loc_1_1))
                (goal))
            (not (occupancy loc_1_2))
            (occupancy loc_1_1)
            (not (ball_at ball_2 loc_1_2))
            (ball_at ball_2 loc_1_1)
            (when
                (and
                    (not (ball_at ball_0 loc_1_2))
                    (not (ball_at ball_1 loc_1_2)))
                (and
                    (not (character_at loc_1_3))
                    (character_at loc_1_2)))
            (not (snow loc_1_1))
            (when
                (and
                    (snow loc_1_1)
                    (ball_size_small ball_2))
                (and
                    (not (ball_size_small ball_2))
                    (ball_size_medium ball_2)))
            (when
                (and
                    (snow loc_1_1)
                    (ball_size_medium ball_2))
                (and                    (not (ball_size_medium ball_2))
                    (ball_size_large ball_2))))
    )

    (:action move_ball__ball_0__loc_1_4__loc_1_3__loc_1_2__ball_1__ball_2

     :parameters
        ()

     :precondition
        (and
            (ball_at ball_0 loc_1_3)
            (character_at loc_1_4)
            (and
                (or
                    (not (ball_at ball_1 loc_1_3))
                    (or
                        (and
                            (ball_size_small ball_0)
                            (ball_size_medium ball_1))
                        (and
                            (ball_size_small ball_0)
                            (ball_size_large ball_1))
                        (and
                            (ball_size_medium ball_0)
                            (ball_size_large ball_1))))
                (or
                    (not (ball_at ball_2 loc_1_3))
                    (or
                        (and
                            (ball_size_small ball_0)
                            (ball_size_medium ball_2))
                        (and
                            (ball_size_small ball_0)
                            (ball_size_large ball_2))
                        (and
                            (ball_size_medium ball_0)
                            (ball_size_large ball_2)))))
            (or
                (not (or
                    (ball_at ball_1 loc_1_3)
                    (ball_at ball_2 loc_1_3)))
                (and

                    (not (ball_at ball_1 loc_1_2))
                    (not (ball_at ball_2 loc_1_2))))
            (and
                (or
                    (not (ball_at ball_1 loc_1_2))
                    (or
                        (and
                            (ball_size_small ball_0)
                            (ball_size_medium ball_1))
                        (and
                            (ball_size_small ball_0)
                            (ball_size_large ball_1))
                        (and
                            (ball_size_medium ball_0)
                            (ball_size_large ball_1))))
                (or
                    (not (ball_at ball_2 loc_1_2))
                    (or
                        (and
                            (ball_size_small ball_0)
                            (ball_size_medium ball_2))
                        (and
                            (ball_size_small ball_0)
                            (ball_size_large ball_2))
                        (and
                            (ball_size_medium ball_0)
                            (ball_size_large ball_2))))))

     :effect
        (and
            (when
                (and
                    (ball_at ball_1 loc_1_2)
                    (ball_at ball_2 loc_1_2))
                (goal))
            (not (occupancy loc_1_3))
            (occupancy loc_1_2)
            (not (ball_at ball_0 loc_1_3))
            (ball_at ball_0 loc_1_2)
            (when
                (and
                    (not (ball_at ball_1 loc_1_3))
                    (not (ball_at ball_2 loc_1_3)))
                (and
                    (not (character_at loc_1_4))
                    (character_at loc_1_3)))
            (not (snow loc_1_2))
            (when
                (and
                    (snow loc_1_2)
                    (ball_size_small ball_0))
                (and
                    (not (ball_size_small ball_0))
                    (ball_size_medium ball_0)))
            (when
                (and
                    (snow loc_1_2)
                    (ball_size_medium ball_0))
                (and                    (not (ball_size_medium ball_0))
                    (ball_size_large ball_0))))
    )

    (:action move_ball__ball_1__loc_1_4__loc_1_3__loc_1_2__ball_0__ball_2

     :parameters
        ()

     :precondition
        (and
            (ball_at ball_1 loc_1_3)
            (character_at loc_1_4)
            (and
                (or
                    (not (ball_at ball_0 loc_1_3))
                    (or
                        (and
                            (ball_size_small ball_1)
                            (ball_size_medium ball_0))
                        (and
                            (ball_size_small ball_1)
                            (ball_size_large ball_0))
                        (and
                            (ball_size_medium ball_1)
                            (ball_size_large ball_0))))
                (or
                    (not (ball_at ball_2 loc_1_3))
                    (or
                        (and
                            (ball_size_small ball_1)
                            (ball_size_medium ball_2))
                        (and
                            (ball_size_small ball_1)
                            (ball_size_large ball_2))
                        (and
                            (ball_size_medium ball_1)
                            (ball_size_large ball_2)))))
            (or
                (not (or
                    (ball_at ball_0 loc_1_3)
                    (ball_at ball_2 loc_1_3)))
                (and

                    (not (ball_at ball_0 loc_1_2))
                    (not (ball_at ball_2 loc_1_2))))
            (and
                (or
                    (not (ball_at ball_0 loc_1_2))
                    (or
                        (and
                            (ball_size_small ball_1)
                            (ball_size_medium ball_0))
                        (and
                            (ball_size_small ball_1)
                            (ball_size_large ball_0))
                        (and
                            (ball_size_medium ball_1)
                            (ball_size_large ball_0))))
                (or
                    (not (ball_at ball_2 loc_1_2))
                    (or
                        (and
                            (ball_size_small ball_1)
                            (ball_size_medium ball_2))
                        (and
                            (ball_size_small ball_1)
                            (ball_size_large ball_2))
                        (and
                            (ball_size_medium ball_1)
                            (ball_size_large ball_2))))))

     :effect
        (and
            (when
                (and
                    (ball_at ball_0 loc_1_2)
                    (ball_at ball_2 loc_1_2))
                (goal))
            (not (occupancy loc_1_3))
            (occupancy loc_1_2)
            (not (ball_at ball_1 loc_1_3))
            (ball_at ball_1 loc_1_2)
            (when
                (and
                    (not (ball_at ball_0 loc_1_3))
                    (not (ball_at ball_2 loc_1_3)))
                (and
                    (not (character_at loc_1_4))
                    (character_at loc_1_3)))
            (not (snow loc_1_2))
            (when
                (and
                    (snow loc_1_2)
                    (ball_size_small ball_1))
                (and
                    (not (ball_size_small ball_1))
                    (ball_size_medium ball_1)))
            (when
                (and
                    (snow loc_1_2)
                    (ball_size_medium ball_1))
                (and                    (not (ball_size_medium ball_1))
                    (ball_size_large ball_1))))
    )

    (:action move_ball__ball_2__loc_1_4__loc_1_3__loc_1_2__ball_0__ball_1

     :parameters
        ()

     :precondition
        (and
            (ball_at ball_2 loc_1_3)
            (character_at loc_1_4)
            (and
                (or
                    (not (ball_at ball_0 loc_1_3))
                    (or
                        (and
                            (ball_size_small ball_2)
                            (ball_size_medium ball_0))
                        (and
                            (ball_size_small ball_2)
                            (ball_size_large ball_0))
                        (and
                            (ball_size_medium ball_2)
                            (ball_size_large ball_0))))
                (or
                    (not (ball_at ball_1 loc_1_3))
                    (or
                        (and
                            (ball_size_small ball_2)
                            (ball_size_medium ball_1))
                        (and
                            (ball_size_small ball_2)
                            (ball_size_large ball_1))
                        (and
                            (ball_size_medium ball_2)
                            (ball_size_large ball_1)))))
            (or
                (not (or
                    (ball_at ball_0 loc_1_3)
                    (ball_at ball_1 loc_1_3)))
                (and

                    (not (ball_at ball_0 loc_1_2))
                    (not (ball_at ball_1 loc_1_2))))
            (and
                (or
                    (not (ball_at ball_0 loc_1_2))
                    (or
                        (and
                            (ball_size_small ball_2)
                            (ball_size_medium ball_0))
                        (and
                            (ball_size_small ball_2)
                            (ball_size_large ball_0))
                        (and
                            (ball_size_medium ball_2)
                            (ball_size_large ball_0))))
                (or
                    (not (ball_at ball_1 loc_1_2))
                    (or
                        (and
                            (ball_size_small ball_2)
                            (ball_size_medium ball_1))
                        (and
                            (ball_size_small ball_2)
                            (ball_size_large ball_1))
                        (and
                            (ball_size_medium ball_2)
                            (ball_size_large ball_1))))))

     :effect
        (and
            (when
                (and
                    (ball_at ball_0 loc_1_2)
                    (ball_at ball_1 loc_1_2))
                (goal))
            (not (occupancy loc_1_3))
            (occupancy loc_1_2)
            (not (ball_at ball_2 loc_1_3))
            (ball_at ball_2 loc_1_2)
            (when
                (and
                    (not (ball_at ball_0 loc_1_3))
                    (not (ball_at ball_1 loc_1_3)))
                (and
                    (not (character_at loc_1_4))
                    (character_at loc_1_3)))
            (not (snow loc_1_2))
            (when
                (and
                    (snow loc_1_2)
                    (ball_size_small ball_2))
                (and
                    (not (ball_size_small ball_2))
                    (ball_size_medium ball_2)))
            (when
                (and
                    (snow loc_1_2)
                    (ball_size_medium ball_2))
                (and                    (not (ball_size_medium ball_2))
                    (ball_size_large ball_2))))
    )
)