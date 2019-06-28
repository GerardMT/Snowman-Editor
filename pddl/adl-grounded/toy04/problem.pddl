(define (problem snowman_problem)

    (:domain snowman_adl_grounded)

    (:init
        (character_at loc_3_2)
        (ball_at ball_0 loc_1_3)
        (ball_size_large ball_0)
        (ball_at ball_1 loc_2_2)
        (ball_size_small ball_1)
        (ball_at ball_2 loc_2_3)
        (ball_size_small ball_2)
        (snow loc_1_2)
        (occupancy loc_1_3)
        (occupancy loc_2_2)
        (occupancy loc_2_3)
    )

    (:goal
        (goal)
    )
)