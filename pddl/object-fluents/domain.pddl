(define (domain snowman_object_fluents)

    (:requirements 
        :typing
        :negative-preconditions
        :equality
        :disjunctive-preconditions
        :conditional-effects
        :object-fluents
        :action-costs
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
        (character_at) - location
        (ball_at ?b - ball) - location
        (next_up ?l - location) - location
        (next_down ?l - location) - location
        (next_right ?l - location) - location
        (next_left ?l - location) - location
        (total-cost) - number
    )
    
    (:action move_up_character

     :parameters
        ()

     :precondition 
        (not (occupancy (next_up (character_at))))

     :effect
        (assign (character_at) (next_up (character_at)))
    )

    (:action move_down_character
        
     :parameters
        ()

     :precondition
        (not (occupancy (next_down (character_at))))
        
     :effect
        (assign (character_at) (next_down (character_at)))
    )

    (:action move_right_character
        
     :parameters
        ()
  
     :precondition
        (not (occupancy (next_right (character_at))))
        
     :effect
        (assign (character_at) (next_right (character_at)))
    )

    (:action move_left_character
        
     :parameters
        ()
  
     :precondition
        (not (occupancy (next_left (character_at))))
        
     :effect
        (assign (character_at) (next_left (character_at)))
    )
    
    (:action move_up_ball

     :parameters
        ()

     :precondition
        (and
            (= (character_at) (next_down (ball_at ?b)))
            (forall (?o - ball)
                (or 
                    (= ?o ?b)
                    (or 
                        (not (= (ball_at ?o) (ball_at ?b)))
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
                        (not (= (ball_at ?o) ((ball_at ?b))))))
                (forall (?o - ball)
                        (not (= (ball_at ?o) (next_up (ball_at ?b))))))
            (forall (?o - ball)
                    (or
                        (not (= (ball_at ?o) (next_up (ball_at ?b))))
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
                    (= (ball_at ?o) (next_up (ball_at ?b)))))
                (goal))
            (occupancy (next_up (ball_at ?b)))
            (assign (ball_at ?b) (next_up (ball_at ?b)))
            (when
                (forall (?o - ball)
                    (or 
                        (= ?o ?b)
                        (not (= (ball_at ?o) (ball_at ?b)))))
                (and
                    (assign (character_at) (ball_at ?b))
                    (not (occupancy (ball_at ?b)))))
            (not (snow (next_up (ball_at ?b))))
            (when
                (and
                    (snow (next_up (ball_at ?b)))
                    (ball-size-small ?b))
                (and
                    (not (ball-size-small ?b))
                    (ball-size-medium ?b)))
            (when
                (and
                    (snow (next_up (ball_at ?b)))
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
            (= (character_at) (next_up (ball_at ?b)))
            (forall (?o - ball)
                (or 
                    (= ?o ?b)
                    (or 
                        (not (= (ball_at ?o) (ball_at ?b)))
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
                        (not (= (ball_at ?o) ((ball_at ?b))))))
                (forall (?o - ball)
                        (not (= (ball_at ?o) (next_down (ball_at ?b))))))
            (forall (?o - ball)
                    (or
                        (not (= (ball_at ?o) (next_down (ball_at ?b))))
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
                    (= (ball_at ?o) (next_down (ball_at ?b)))))
                (goal))
            (occupancy (next_down (ball_at ?b)))
            (assign (ball_at ?b) (next_down (ball_at ?b)))
            (when
                (forall (?o - ball)
                    (or 
                        (= ?o ?b)
                        (not (= (ball_at ?o) (ball_at ?b)))))
                (and
                    (assign (character_at) (ball_at ?b))
                    (not (occupancy (ball_at ?b)))))
            (not (snow (next_down (ball_at ?b))))
            (when
                (and
                    (snow (next_down (ball_at ?b)))
                    (ball-size-small ?b))
                (and
                    (not (ball-size-small ?b))
                    (ball-size-medium ?b)))
            (when
                (and
                    (snow (next_down (ball_at ?b)))
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
            (= (character_at) (next_left (ball_at ?b)))
            (forall (?o - ball)
                (or 
                    (= ?o ?b)
                    (or 
                        (not (= (ball_at ?o) (ball_at ?b)))
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
                        (not (= (ball_at ?o) ((ball_at ?b))))))
                (forall (?o - ball)
                        (not (= (ball_at ?o) (next_right (ball_at ?b))))))
            (forall (?o - ball)
                    (or
                        (not (= (ball_at ?o) (next_right (ball_at ?b))))
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
                    (= (ball_at ?o (next_right (ball_at ?b))))))
                (goal))
            (occupancy (next_right (ball_at ?b)))
            (assign (ball_at ?b) (next_right (ball_at ?b)))
            (when
                (forall (?o - ball)
                    (or 
                        (= ?o ?b)
                        (not (= (ball_at ?o) (ball_at ?b)))))
                (and
                    (assign (character_at) (ball_at ?b))
                    (not (occupancy (ball_at ?b)))))
            (not (snow (next_right (ball_at ?b))))
            (when
                (and
                    (snow (next_right (ball_at ?b)))
                    (ball-size-small ?b))
                (and
                    (not (ball-size-small ?b))
                    (ball-size-medium ?b)))
            (when
                (and
                    (snow (next_right (ball_at ?b)))
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
            (= (character_at) (next_right (ball_at ?b)))
            (forall (?o - ball)
                (or 
                    (= ?o ?b)
                    (or 
                        (not (= (ball_at ?o) (ball_at ?b)))
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
                        (not (= (ball_at ?o) ((ball_at ?b))))))
                (forall (?o - ball)
                        (not (= (ball_at ?o) (next_left (ball_at ?b))))))
            (forall (?o - ball)
                    (or
                        (not (= (ball_at ?o) (next_left (ball_at ?b))))
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
                    (= (ball_at ?o) (next_left (ball_at ?b)))))
                (goal))
            (occupancy (next_left (ball_at ?b)))
            (assign (ball_at ?b) (next_left (ball_at ?b)))
            (when
                (forall (?o - ball)
                    (or 
                        (= ?o ?b)
                        (not (= (ball_at ?o) (ball_at ?b)))))
                (and
                    (assign (character_at) (ball_at ?b))
                    (not (occupancy (ball_at ?b)))))
            (not (snow (next_left (ball_at ?b))))
            (when
                (and
                    (snow (next_left (ball_at ?b)))
                    (ball-size-small ?b))
                (and
                    (not (ball-size-small ?b))
                    (ball-size-medium ?b)))
            (when
                (and
                    (snow (next_left (ball_at ?b)))
                    (ball-size-medium ?b))
                (and
                    (not (ball-size-medium ?b))
                    (ball-size-large ?b))))
    )
)
