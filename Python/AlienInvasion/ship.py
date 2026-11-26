import pygame
from settings import Settings

class Ship():

    def __init__(self,screen):
        self.screen = screen
        self.moving_right = False
        self.moving_left  = False
        self.setting = Settings()

        #Load the ship image
        self.image = pygame.image.load(self.setting.ship_image)
        self.rect  = self.image.get_rect()
        self.screen_rect = screen.get_rect()

        #Start the ship at the bottom
        self.rect.midbottom = self.screen_rect.midbottom


    def update(self):
        """Update the ship's position according to the movement flags"""

        if self.moving_right and self.rect.right < self.screen_rect.right:
            self.rect.centerx += self.setting.ship_speed_factor
        if self.moving_left and self.rect.left > 0:
            self.rect.centerx -= self.setting.ship_speed_factor

    def blitme(self):
        self.screen.blit(self.image, self.rect)