import pygame
import sys
from settings import Settings
from ship import Ship
import game_functions as gf

class alien():
    def __init__(self):
        #Initializing the screen 
        pygame.init()
        self.setting = Settings()
        self.screen = pygame.display.set_mode(self.setting.screen_size)
        self.space_ship = Ship(self.screen)

    def run_game(self):
        pygame.display.set_caption("Alien Invasion")

        #Main game loop 
        while True:
            #Lookout for any events

            gf.check_events(self.space_ship)
            self.space_ship.update()
            gf.update_screen(self.setting, self.screen, self.space_ship)
            



if __name__ == '__main__':
    ai = alien()
    ai.run_game()