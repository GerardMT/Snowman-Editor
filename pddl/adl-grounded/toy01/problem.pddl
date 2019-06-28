(define (problem snowman_problem)

    (:domain snowman_adl_grounded)

    (:init
        (character_at loc_1_1)
        (ball_at ball_0 loc_1_2)
        (ball_size_small ball_0)
        (ball_at ball_1 loc_1_3)
        (ball_size_large ball_1)
        (ball_at ball_2 loc_1_3)
        (ball_size_medium ball_2)
        (occupancy loc_1_2)
        (occupancy loc_1_3)
    )

    (:goal
        (goal)
    )
)