import sys
import pygame

def check_events(space_ship):
    """Responds to keypresses"""
    for event in pygame.event.get():
        if event.type == pygame.QUIT:
            sys.exit()
        elif event.type == pygame.KEYDOWN:
            if event.key == pygame.K_LEFT:
                space_ship.moving_left = True
            elif event.key == pygame.K_RIGHT:
                space_ship.moving_right = True

        elif event.type == pygame.KEYUP:
            if event.key == pygame.K_LEFT:
                space_ship.moving_left = False
            elif event.key == pygame.K_RIGHT:
                space_ship.moving_right = False


def update_screen(settings, screen, ship):
    """Updates the screen on change"""
    screen.fill(settings.bg_color)
    ship.blitme()

    pygame.display.flip()