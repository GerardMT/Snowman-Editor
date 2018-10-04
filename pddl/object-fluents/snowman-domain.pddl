(define (domain snowman-object-fluents)
    
    (:requirements 
        :typing
        :negative-preconditions
        :equality
        :disjunctive-preconditions
        :conditional-effects
        :object-fluets
    )

    (:types 
        location ball - object
    )

    (:predicates
        (snow ?l - location)
        (occupancy ?l - location)
        (goal)
    )
    
    (:functions
        (character-at) - location
        (ball-at ?b - ball) - location
        (next-up ?l - location) - location
        (next-down ?l - location) - location
        (next-right ?l - location) - location
        (next-left ?l - location) - location
    )
    
    (:action move_up_character
        
     :parameters
        
     :precondition
        (not (occupancy (next-up (character-at))))

     :effect
        (assign (character-at) (next-up (character-at)))
    )

    (:action move_down_character
        
     :parameters
        
     :precondition
        (not (occupancy (next-down (character-at))))
        
     :effect
        (assign (character-at) (next-down (character-at)))
    )

    (:action move_right_character
        
     :parameters
        
     :precondition
        (not (occupancy (next-right (character-at))))
        
     :effect
        (assign (character-at) (next-right (character-at)))
    )

    (:action move_left_character
        
     :parameters
        
     :precondition
        (not (occupancy (next-left (character-at))))
        
     :effect
        (assign (character-at) (next-left (character-at)))
    )
    
    (:action move_up_ball

     :parameters
        (?b - ball)

     :precondition
        (and
            (= (character-at) (next-down (ball-at ?b)))
            (forall (?o - ball)
                (or 
                    (= ?o ?b)
                    (or 
                        (not (= (ball-at ?o) (ball-at ?b)))
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
                        (not (= (ball-at ?o) ((ball-at ?b))))))
                (forall (?o - ball)
                        (not (= (ball-at ?o) (next-up (ball-at ?b))))))
            (forall (?o - ball)
                    (or
                        (not (= (ball-at ?o) (next-up (ball-at ?b))))
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
                    (= (ball-at ?o) (next-up (ball-at ?b)))))
                (goal))
            (not (occupancy (ball-at ?b)))
            (occupancy (next-up (ball-at ?b)))
            (assign (ball-at ?b) (next-up (ball-at ?b)))
            (when
                (forall (?o - ball)
                    (or 
                        (= ?o ?b)
                        (not (= (ball-at ?o) (ball-at ?b)))))
                (assign (character-at) (ball-at ?b)))
            (not (snow (next-up (ball-at ?b))))
            (when
                (and
                    (snow (next-up (ball-at ?b)))
                    (ball-size-small ?b))
                (and
                    (not (ball-size-small ?b))
                    (ball-size-medium ?b)))
            (when
                (and
                    (snow (next-up (ball-at ?b)))
                    (ball-size-medium ?b))
                (and
                    (not (ball-size-medium ?b))
                    (ball-size-large ?b))))
    )

    (:action move_down_ball

     :parameters
        (?b - ball)

     :precondition
        (and
            (= (character-at) (next-up (ball-at ?b)))
            (forall (?o - ball)
                (or 
                    (= ?o ?b)
                    (or 
                        (not (= (ball-at ?o) (ball-at ?b)))
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
                        (not (= (ball-at ?o) ((ball-at ?b))))))
                (forall (?o - ball)
                        (not (= (ball-at ?o) (next-down (ball-at ?b))))))
            (forall (?o - ball)
                    (or
                        (not (= (ball-at ?o) (next-down (ball-at ?b))))
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
                    (= (ball-at ?o) (next-down (ball-at ?b)))))
                (goal))
            (not (occupancy (ball-at ?b)))
            (occupancy (next-down (ball-at ?b)))
            (assign (ball-at ?b) (next-down (ball-at ?b)))
            (when
                (forall (?o - ball)
                    (or 
                        (= ?o ?b)
                        (not (= (ball-at ?o) (ball-at ?b)))))
                (assign (character-at) (ball-at ?b)))
            (not (snow (next-down (ball-at ?b))))
            (when
                (and
                    (snow (next-down (ball-at ?b)))
                    (ball-size-small ?b))
                (and
                    (not (ball-size-small ?b))
                    (ball-size-medium ?b)))
            (when
                (and
                    (snow (next-down (ball-at ?b)))
                    (ball-size-medium ?b))
                (and
                    (not (ball-size-medium ?b))
                    (ball-size-large ?b))))
    )

    (:action move_right_ball

     :parameters
        (?b - ball)

     :precondition
        (and
            (= (character-at) (next-left (ball-at ?b)))
            (forall (?o - ball)
                (or 
                    (= ?o ?b)
                    (or 
                        (not (= (ball-at ?o) (ball-at ?b)))
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
                        (not (= (ball-at ?o) ((ball-at ?b))))))
                (forall (?o - ball)
                        (not (= (ball-at ?o) (next-right (ball-at ?b))))))
            (forall (?o - ball)
                    (or
                        (not (= (ball-at ?o) (next-right (ball-at ?b))))
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
                    (= (ball-at ?o (next-right (ball-at ?b))))))
                (goal))
            (not (occupancy (ball-at ?b)))
            (occupancy (next-right (ball-at ?b)))
            (assign (ball-at ?b) (next-right (ball-at ?b)))
            (when
                (forall (?o - ball)
                    (or 
                        (= ?o ?b)
                        (not (= (ball-at ?o) (ball-at ?b)))))
                (assign (character-at) (ball-at ?b)))
            (not (snow (next-right (ball-at ?b))))
            (when
                (and
                    (snow (next-right (ball-at ?b)))
                    (ball-size-small ?b))
                (and
                    (not (ball-size-small ?b))
                    (ball-size-medium ?b)))
            (when
                (and
                    (snow (next-right (ball-at ?b)))
                    (ball-size-medium ?b))
                (and
                    (not (ball-size-medium ?b))
                    (ball-size-large ?b))))
    )

    (:action move_left_ball

     :parameters
        (?b - ball)

     :precondition
        (and
            (= (character-at) (next-right (ball-at ?b)))
            (forall (?o - ball)
                (or 
                    (= ?o ?b)
                    (or 
                        (not (= (ball-at ?o) (ball-at ?b)))
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
                        (not (= (ball-at ?o) ((ball-at ?b))))))
                (forall (?o - ball)
                        (not (= (ball-at ?o) (next-left (ball-at ?b))))))
            (forall (?o - ball)
                    (or
                        (not (= (ball-at ?o) (next-left (ball-at ?b))))
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
                    (= (ball-at ?o) (next-left (ball-at ?b)))))
                (goal))
            (not (occupancy (ball-at ?b)))
            (occupancy (next-left (ball-at ?b)))
            (assign (ball-at ?b) (next-left (ball-at ?b)))
            (when
                (forall (?o - ball)
                    (or 
                        (= ?o ?b)
                        (not (= (ball-at ?o) (ball-at ?b)))))
                (assign (character-at) (ball-at ?b)))
            (not (snow (next-left (ball-at ?b))))
            (when
                (and
                    (snow (next-left (ball-at ?b)))
                    (ball-size-small ?b))
                (and
                    (not (ball-size-small ?b))
                    (ball-size-medium ?b)))
            (when
                (and
                    (snow (next-left (ball-at ?b)))
                    (ball-size-medium ?b))
                (and
                    (not (ball-size-medium ?b))
                    (ball-size-large ?b))))
    )
)
