(define (problem snowman_problem)

    (:domain snowman_adl_grounded)

    (:init
        (character_at loc_1_4)
        (ball_at ball_0 loc_2_3)
        (ball_size_small ball_0)
        (ball_at ball_1 loc_2_4)
        (ball_size_small ball_1)
        (ball_at ball_2 loc_4_3)
        (ball_size_small ball_2)
        (snow loc_3_3)
        (snow loc_3_4)
        (snow loc_4_4)
        (occupancy loc_2_3)
        (occupancy loc_2_4)
        (occupancy loc_4_3)
    )

    (:goal
        (goal)
    )
)