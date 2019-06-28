(define (problem toy01)
    (:domain snowman_adl)
    (:objects
        dir_right - direction
        dir_left - direction
        dir_up - direction
        dir_down - direction
        ball_0 - ball
        ball_1 - ball
        ball_2 - ball
        loc_1_1 - location
        loc_1_2 - location
        loc_1_3 - location
    )
    (:init
        (next loc_1_1 loc_1_2 dir_up)
        (next loc_1_2 loc_1_3 dir_up)
        (next loc_1_2 loc_1_1 dir_down)
        (next loc_1_3 loc_1_2 dir_down)
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
