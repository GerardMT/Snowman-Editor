(define (problem toy05)
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
        loc_1_4 - location
        loc_2_1 - location
        loc_2_2 - location
        loc_2_3 - location
        loc_2_4 - location
        loc_3_1 - location
        loc_3_2 - location
        loc_3_3 - location
        loc_3_4 - location
        loc_4_1 - location
        loc_4_2 - location
        loc_4_3 - location
        loc_4_4 - location
    )
    (:init
        (next loc_1_1 loc_2_1 dir_right)
        (next loc_1_1 loc_1_2 dir_up)
        (next loc_1_2 loc_2_2 dir_right)
        (next loc_1_2 loc_1_3 dir_up)
        (next loc_1_2 loc_1_1 dir_down)
        (next loc_1_3 loc_2_3 dir_right)
        (next loc_1_3 loc_1_4 dir_up)
        (next loc_1_3 loc_1_2 dir_down)
        (next loc_1_4 loc_2_4 dir_right)
        (next loc_1_4 loc_1_3 dir_down)
        (next loc_2_1 loc_3_1 dir_right)
        (next loc_2_1 loc_1_1 dir_left)
        (next loc_2_1 loc_2_2 dir_up)
        (next loc_2_2 loc_3_2 dir_right)
        (next loc_2_2 loc_1_2 dir_left)
        (next loc_2_2 loc_2_3 dir_up)
        (next loc_2_2 loc_2_1 dir_down)
        (next loc_2_3 loc_3_3 dir_right)
        (next loc_2_3 loc_1_3 dir_left)
        (next loc_2_3 loc_2_4 dir_up)
        (next loc_2_3 loc_2_2 dir_down)
        (next loc_2_4 loc_3_4 dir_right)
        (next loc_2_4 loc_1_4 dir_left)
        (next loc_2_4 loc_2_3 dir_down)
        (next loc_3_1 loc_4_1 dir_right)
        (next loc_3_1 loc_2_1 dir_left)
        (next loc_3_1 loc_3_2 dir_up)
        (next loc_3_2 loc_4_2 dir_right)
        (next loc_3_2 loc_2_2 dir_left)
        (next loc_3_2 loc_3_3 dir_up)
        (next loc_3_2 loc_3_1 dir_down)
        (next loc_3_3 loc_4_3 dir_right)
        (next loc_3_3 loc_2_3 dir_left)
        (next loc_3_3 loc_3_4 dir_up)
        (next loc_3_3 loc_3_2 dir_down)
        (next loc_3_4 loc_4_4 dir_right)
        (next loc_3_4 loc_2_4 dir_left)
        (next loc_3_4 loc_3_3 dir_down)
        (next loc_4_1 loc_3_1 dir_left)
        (next loc_4_1 loc_4_2 dir_up)
        (next loc_4_2 loc_3_2 dir_left)
        (next loc_4_2 loc_4_3 dir_up)
        (next loc_4_2 loc_4_1 dir_down)
        (next loc_4_3 loc_3_3 dir_left)
        (next loc_4_3 loc_4_4 dir_up)
        (next loc_4_3 loc_4_2 dir_down)
        (next loc_4_4 loc_3_4 dir_left)
        (next loc_4_4 loc_4_3 dir_down)
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
